package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Dodatnia kwota to wpłata, ujemna to wypłata z celu. */
public record GoalContributionRequest(@NotNull(message = "Amount cannot be null") BigDecimal amount) {
}
