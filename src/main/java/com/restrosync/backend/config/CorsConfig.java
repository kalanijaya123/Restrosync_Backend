package com.restrosync.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins != null && !allowedOrigins.isEmpty()
                ? allowedOrigins.split(",")
                : new String[] { "*" };

        CorsRegistration mapping = registry.addMapping("/api/**")
                .allowedMethods("*")
                .allowedHeaders("*");

        boolean allowAnyOrigin = origins.length == 1 && "*".equals(origins[0]);
        if (allowAnyOrigin) {
            mapping.allowedOrigins("*").allowCredentials(false);
        } else {
            mapping.allowedOriginPatterns(origins).allowCredentials(true);
        }
    }
}