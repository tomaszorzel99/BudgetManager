package com.personalfinance.BudgetManager.DTO;

import java.math.BigDecimal;

public record CurrencyTotalsDTO(String currency, BigDecimal income, BigDecimal expense) {
}