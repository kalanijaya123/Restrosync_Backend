package com.restrosync.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.email.from:}")
    private String fromAddress;

    @Value("${spring.mail.host:}")
    private String mailHost;

    public void sendInvoiceEmail(String emailAddress, String invoiceReference, String customerName, double total,
            String paymentMethod, List<String> itemLines, String notes) {
        if (!StringUtils.hasText(emailAddress)) {
            log.info("Skipping email invoice notification because no customer email was provided");
            return;
        }

        String normalizedEmail = emailAddress.trim();
        String subject = "RestroSync invoice " + invoiceReference;
        String body = buildInvoiceMessage(invoiceReference, customerName, total, paymentMethod, itemLines, notes);
        sendMessage(normalizedEmail, subject, body);
    }

    private String buildInvoiceMessage(String invoiceReference, String customerName, double total,
            String paymentMethod, List<String> itemLines, String notes) {
        StringBuilder message = new StringBuilder();
        message.append("RestroSync invoice ").append(invoiceReference).append('\n');

        if (StringUtils.hasText(customerName)) {
            message.append("Customer: ").append(customerName.trim()).append('\n');
        }

        message.append("Total: Rs ").append(String.format("%.0f", total)).append('\n');

        if (StringUtils.hasText(paymentMethod)) {
            message.append("Paid via: ").append(paymentMethod.trim()).append('\n');
        }

        if (itemLines != null && !itemLines.isEmpty()) {
            message.append("Items:\n");
            for (String line : itemLines) {
                message.append("- ").append(line).append('\n');
            }
        }

        if (StringUtils.hasText(notes)) {
            message.append("Note: ").append(notes.trim()).append('\n');
        }

        message.append('\n').append("Thank you for ordering with RestroSync.");
        return message.toString();
    }

    private void sendMessage(String emailAddress, String subject, String message) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!emailEnabled || mailSender == null || !StringUtils.hasText(mailHost)) {
            log.info("Email notification disabled or not configured. Would send to {}: {}", emailAddress, subject);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());
            helper.setTo(emailAddress);
            helper.setSubject(subject);
            helper.setText(message, false);

            if (StringUtils.hasText(fromAddress)) {
                helper.setFrom(fromAddress.trim());
            }

            mailSender.send(mimeMessage);
            log.info("Invoice email sent to {}", emailAddress);
        } catch (Exception e) {
            log.error("Failed to send invoice email to {}", emailAddress, e);
        }
    }
}