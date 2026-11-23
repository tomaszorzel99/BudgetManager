package com.personalfinance.BudgetManager.DTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Historia salda konta. openingBalance to różnica między obecnym saldem a sumą operacji
 * (zwykle 0; inna wartość oznacza saldo sprzed historii zapisanej w aplikacji).
 */
public record AccountHistoryDTO(
        Long accountId,
        String accountName,
        String currency,
        BigDecimal currentBalance,
        BigDecimal openingBalance,
        List<AccountHistoryEntryDTO> entries) {
}
