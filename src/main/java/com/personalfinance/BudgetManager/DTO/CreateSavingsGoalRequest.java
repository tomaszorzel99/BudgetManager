package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateSavingsGoalRequest(
        @NotBlank(message = "Goal name cannot be blank")
        @Size(max = 100, message = "Goal name is too long") String name,
        @NotNull(message = "Target amount cannot be null")
        @DecimalMin(value = "0.01", message = "Target amount must be greater than 0") BigDecimal targetAmount,
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a 3-letter code") String currency,
        LocalDate targetDate,
        Long accountId,
        @DecimalMin(value = "0.00", message = "Initial amount cannot be negative") BigDecimal initialAmount,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a hex value like #FF6A3D") String color) {
}
