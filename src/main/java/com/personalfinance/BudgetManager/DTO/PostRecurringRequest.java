package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Księgowanie najbliższego wystąpienia; puste pola = wartości z szablonu. */
public record PostRecurringRequest(
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0") BigDecimal amount,
        LocalDate date) {
}
