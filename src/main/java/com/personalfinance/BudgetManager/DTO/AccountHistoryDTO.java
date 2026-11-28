package com.personalfinance.BudgetManager.DTO;

import java.math.BigDecimal;
import java.util.List;

public record AccountHistoryDTO(
        Long accountId,
        String accountName,
        String currency,
        BigDecimal currentBalance,
        BigDecimal openingBalance,
        List<AccountHistoryEntryDTO> entries) {
}
