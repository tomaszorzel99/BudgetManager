package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.CategoryType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpcomingPaymentDTO(
        Long recurringId,
        LocalDate date,
        CategoryType type,
        BigDecimal amount,
        String currency,
        String description,
        String categoryName,
        String categoryColor,
        String accountName,
        boolean autoPost,
        boolean overdue,
        boolean next) {
}
