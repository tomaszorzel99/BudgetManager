package com.personalfinance.BudgetManager.Configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        for (String origin : allowedOrigins) {
            if (origin.isBlank() || origin.contains("*") || origin.equalsIgnoreCase("null")) {
                throw new IllegalStateException(
                        "app.cors.allowed-origins: niedozwolona wartość '" + origin
                                + "'. Podaj pełne adresy, np. http://localhost:5500");
            }
        }
    }
}
