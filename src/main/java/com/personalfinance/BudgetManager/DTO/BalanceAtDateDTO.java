package com.personalfinance.BudgetManager.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BalanceAtDateDTO (
        LocalDate balanceDate,
        String currency,
        BigDecimal totalBalance,
        BigDecimal savingsBalance,
        BigDecimal investmentBalance,
        /** Suma sald kont kredytów (≤ 0). totalBalance zawiera ją już, więc to wartość netto. */
        BigDecimal liabilitiesBalance,
        /** Wartość nieruchomości (≥ 0), także zawarta w totalBalance. Majątek płynny = total − liabilities − property. */
        BigDecimal propertyBalance){
}