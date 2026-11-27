package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Nowa wycena nieruchomości. Różnica do obecnej wartości zapisuje się jako korekta (może być ujemna),
 * więc spadek wartości nie trafia do wydatków.
 *
 * @param value       wartość nieruchomości na dzień {@code date}
 * @param date        dzień wyceny (domyślnie dziś)
 * @param description opis, np. „Wycena z operatu”
 */
public record PropertyValueRequest(
        @NotNull @DecimalMin(value = "0.00", message = "Property value cannot be negative") BigDecimal value,
        @PastOrPresent(message = "Valuation date cannot be in the future") LocalDate date,
        @Size(max = 255) String description) {
}