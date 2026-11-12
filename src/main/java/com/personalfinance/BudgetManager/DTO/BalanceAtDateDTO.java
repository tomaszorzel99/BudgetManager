package com.personalfinance.BudgetManager.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BalanceAtDateDTO (
    LocalDate balanceDate,
    String currency,
    BigDecimal totalBalance,
    BigDecimal savingsBalance,
    BigDecimal investmentBalance){
}
