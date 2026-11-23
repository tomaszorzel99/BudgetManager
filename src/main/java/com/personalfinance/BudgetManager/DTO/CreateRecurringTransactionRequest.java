package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.CategoryType;
import com.personalfinance.BudgetManager.Model.RecurrenceFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateRecurringTransactionRequest(
        @NotNull(message = "Type cannot be null") CategoryType type,
        @NotNull(message = "Amount cannot be null")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0") BigDecimal amount,
        @Size(max = 255, message = "Description is too long") String description,
        @NotNull(message = "Frequency cannot be null") RecurrenceFrequency frequency,
        @NotNull(message = "Start date cannot be null") LocalDate startDate,
        LocalDate endDate,
        boolean autoPost,
        @NotNull(message = "Account id cannot be null") Long accountId,
        @NotNull(message = "Category id cannot be null") Long categoryId,
        @NotNull(message = "Subcategory id cannot be null") Long subcategoryId) {
}
