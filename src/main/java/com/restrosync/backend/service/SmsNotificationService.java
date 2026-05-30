package com.restrosync.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsNotificationService {

    private final ObjectMapper objectMapper;

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${app.sms.provider-url:}")
    private String providerUrl;

    @Value("${app.sms.api-key:}")
    private String apiKey;

    @Value("${app.sms.sender:RestroSync}")
    private String sender;

    public void sendInvoiceMessage(String phoneNumber, String invoiceReference, String customerName, double total,
            String paymentMethod, List<String> itemLines, String notes) {
        if (!StringUtils.hasText(phoneNumber)) {
            log.info("Skipping SMS notification because no customer phone number was provided");
            return;
        }

        String normalizedPhone = normalizePhone(phoneNumber);
        String message = buildInvoiceMessage(invoiceReference, customerName, total, paymentMethod, itemLines, notes);
        sendMessage(normalizedPhone, message);
    }

    private String buildInvoiceMessage(String invoiceReference, String customerName, double total,
            String paymentMethod, List<String> itemLines, String notes) {
        StringBuilder message = new StringBuilder();
        message.append("RestroSync invoice ").append(invoiceReference);

        if (StringUtils.hasText(customerName)) {
            message.append(" for ").append(customerName.trim());
        }

        message.append(". Total Rs ").append(String.format("%.0f", total));

        if (StringUtils.hasText(paymentMethod)) {
            message.append(". Paid via ").append(paymentMethod.trim());
        }

        if (itemLines != null && !itemLines.isEmpty()) {
            message.append(". Items: ").append(String.join("; ", itemLines));
        }

        if (StringUtils.hasText(notes)) {
            message.append(". Note: ").append(notes.trim());
        }

        return message.toString();
    }

    private void sendMessage(String phoneNumber, String message) {
        if (!smsEnabled || !StringUtils.hasText(providerUrl)) {
            log.info("SMS notification disabled or not configured. Would send to {}: {}", phoneNumber, message);
            return;
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("to", phoneNumber);
            payload.put("from", sender);
            payload.put("message", message);

            String body = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(providerUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

            if (StringUtils.hasText(apiKey)) {
                requestBuilder.header("Authorization", "Bearer " + apiKey.trim());
            }

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("SMS invoice notification sent to {}", phoneNumber);
            } else {
                log.warn("SMS provider returned {} for {}", response.statusCode(), phoneNumber);
            }
        } catch (Exception e) {
            log.error("Failed to send SMS invoice notification to {}", phoneNumber, e);
        }
    }

    private String normalizePhone(String rawPhone) {
        String trimmed = rawPhone.trim();
        if (trimmed.startsWith("+")) {
            return "+" + trimmed.substring(1).replaceAll("\\D", "");
        }
        return trimmed.replaceAll("\\D", "");
    }
}