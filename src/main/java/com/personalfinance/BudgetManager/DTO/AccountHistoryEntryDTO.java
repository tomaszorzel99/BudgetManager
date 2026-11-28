package com.personalfinance.BudgetManager.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountHistoryEntryDTO(
        String kind,
        Long id,
        LocalDate date,
        String type,
        String description,
        String categoryName,
        String subcategoryName,
        String categoryColor,
        String counterparty,
        String userName,
        BigDecimal amount,
        BigDecimal balance) {
}
