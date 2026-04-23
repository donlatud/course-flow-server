package com.techup.course_flow_server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    /**
     * Comma-separated list of allowed origins, e.g.
     * "http://localhost:5173,https://admin.example.com".
     *
     * If you need flexible dev ports, prefer allowed origin patterns
     * (e.g. "http://localhost:*,http://127.0.0.1:*") which works with Vite's
     * auto port switching while still being explicit.
     *
     * For local development we default to Vite dev server ports.
     */
    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:5174}")
    private String allowedOrigins;

    /**
     * Comma-separated allowed origin patterns (supports wildcards),
     * e.g. "http://localhost:*,http://127.0.0.1:*".
     *
     * Default enables local dev regardless of Vite port.
     */
    @Value("${cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private String allowedOriginPatterns;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String[] patterns = splitOrigins(allowedOriginPatterns);
                String[] origins = splitOrigins(allowedOrigins);

                var reg = registry.addMapping("/api/**")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);

                if (patterns.length > 0) {
                    reg.allowedOriginPatterns(patterns);
                } else {
                    reg.allowedOrigins(origins);
                }
            }
        };
    }

    private String[] splitOrigins(String origins) {
        return origins.trim().isEmpty()
                ? new String[0]
                : origins.replace(" ", "").split(",");
    }
}

