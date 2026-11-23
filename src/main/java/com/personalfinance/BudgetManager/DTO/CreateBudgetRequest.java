package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CreateBudgetRequest(
        @NotNull(message = "Category id cannot be null") Long categoryId,
        @NotNull(message = "Amount cannot be null")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0") BigDecimal amount,
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a 3-letter code") String currency) {
}
