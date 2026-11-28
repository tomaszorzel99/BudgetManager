package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Dodatnia kwota to wpłata, ujemna to wypłata z celu. */
public record GoalContributionRequest(
        @NotNull(message = "Amount cannot be null")
        @Digits(integer = 18, fraction = 2, message = "Amount can have at most 2 decimal places") BigDecimal amount) {
}