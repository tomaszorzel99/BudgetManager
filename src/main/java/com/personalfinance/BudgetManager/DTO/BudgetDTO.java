package com.personalfinance.BudgetManager.DTO;

import java.math.BigDecimal;

/**
 * Budżet z wykonaniem w wybranym miesiącu.
 * status: OK (&lt; 80%), WARNING (80–100%), EXCEEDED (&gt; 100%).
 */
public record BudgetDTO(
        Long id,
        Long categoryId,
        String categoryName,
        String categoryColor,
        BigDecimal amount,
        String currency,
        int month,
        int year,
        BigDecimal spent,
        BigDecimal remaining,
        BigDecimal percent,
        String status) {
}
