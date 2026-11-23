package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Zmiana częściowa. clearTargetDate / clearAccount usuwają datę lub powiązanie z kontem. */
public record UpdateSavingsGoalRequest(
        @Size(min = 1, max = 100, message = "Goal name must have 1-100 characters") String name,
        @DecimalMin(value = "0.01", message = "Target amount must be greater than 0") BigDecimal targetAmount,
        LocalDate targetDate,
        Boolean clearTargetDate,
        Long accountId,
        Boolean clearAccount,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a hex value like #FF6A3D") String color) {
}
