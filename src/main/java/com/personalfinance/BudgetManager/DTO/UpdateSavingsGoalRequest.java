package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateSavingsGoalRequest(
        @Size(min = 1, max = 100, message = "Goal name must have 1-100 characters") String name,
        @DecimalMin(value = "0.01", message = "Target amount must be greater than 0")
        @Digits(integer = 18, fraction = 2, message = "Amount can have at most 2 decimal places") BigDecimal targetAmount,
        LocalDate targetDate,
        Boolean clearTargetDate,
        Long accountId,
        Boolean clearAccount,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a hex value like #FF6A3D") String color) {
}