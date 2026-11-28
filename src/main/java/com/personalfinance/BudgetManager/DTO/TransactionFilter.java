package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.CategoryType;

import java.time.LocalDate;

public record TransactionFilter(Long groupId,
                                boolean mine,
                                LocalDate from,
                                LocalDate to,
                                CategoryType type,
                                Long categoryId,
                                Long accountId,
                                String q) {
}