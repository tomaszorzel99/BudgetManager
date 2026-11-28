package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Księgowanie najbliższego wystąpienia; puste pola = wartości z szablonu. */
public record PostRecurringRequest(
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Digits(integer = 18, fraction = 2, message = "Amount can have at most 2 decimal places") BigDecimal amount,
        LocalDate date) {
}