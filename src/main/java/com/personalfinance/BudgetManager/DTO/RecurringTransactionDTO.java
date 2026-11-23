package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.CategoryType;
import com.personalfinance.BudgetManager.Model.RecurrenceFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringTransactionDTO(
        Long id,
        CategoryType type,
        BigDecimal amount,
        String description,
        RecurrenceFrequency frequency,
        LocalDate startDate,
        LocalDate nextDate,
        LocalDate endDate,
        boolean autoPost,
        boolean active,
        Long accountId,
        String accountName,
        String currency,
        Long categoryId,
        String categoryName,
        String categoryColor,
        Long subcategoryId,
        String subcategoryName,
        BigDecimal monthlyEstimate) {
}
