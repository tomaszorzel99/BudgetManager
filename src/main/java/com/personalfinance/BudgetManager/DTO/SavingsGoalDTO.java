package com.personalfinance.BudgetManager.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cel z policzonym postępem. currentAmount to saldo powiązanego konta albo suma wpłat.
 * monthlyNeeded: ile trzeba odkładać miesięcznie, by zdążyć na targetDate (null bez daty).
 */
public record SavingsGoalDTO(
        Long id,
        String name,
        BigDecimal targetAmount,
        BigDecimal savedAmount,
        BigDecimal currentAmount,
        BigDecimal remaining,
        BigDecimal percent,
        String currency,
        LocalDate targetDate,
        Integer monthsLeft,
        BigDecimal monthlyNeeded,
        Long accountId,
        String accountName,
        String color,
        boolean completed,
        LocalDateTime createdAt) {
}
