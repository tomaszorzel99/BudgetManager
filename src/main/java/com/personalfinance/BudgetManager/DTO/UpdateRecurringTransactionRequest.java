package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.RecurrenceFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Zmiana częściowa: pola null pozostają bez zmian. clearEndDate=true usuwa datę końcową. */
public record UpdateRecurringTransactionRequest(
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0") BigDecimal amount,
        @Size(max = 255, message = "Description is too long") String description,
        RecurrenceFrequency frequency,
        LocalDate nextDate,
        LocalDate endDate,
        Boolean clearEndDate,
        Boolean autoPost,
        Boolean active,
        Long accountId,
        Long categoryId,
        Long subcategoryId,
        Boolean clearSubcategory) {
}
