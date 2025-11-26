package com.restrosync.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class RestroSyncApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(RestroSyncApplication.class, args);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/**")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/uploads/menu/")
                .setCachePeriod(0); // Disable cache for testing
    }

    // Keep your CORS filter
    @Bean
    public org.springframework.web.filter.CorsFilter corsFilter() {
        var config = new org.springframework.web.cors.CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:5173");
        config.addAllowedOrigin("http://localhost:5174");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new org.springframework.web.filter.CorsFilter(source);
    }
}