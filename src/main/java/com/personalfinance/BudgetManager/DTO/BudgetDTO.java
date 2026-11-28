package com.personalfinance.BudgetManager.DTO;

import java.math.BigDecimal;

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
