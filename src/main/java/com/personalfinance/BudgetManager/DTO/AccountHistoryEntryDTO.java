package com.personalfinance.BudgetManager.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Jedna operacja na koncie z saldem po jej wykonaniu.
 * kind: TRANSACTION, TRANSFER_IN, TRANSFER_OUT. amount ze znakiem (wydatek i przelew wychodzący &lt; 0).
 */
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
