package com.personalfinance.BudgetManager.Model;

import java.math.BigDecimal;

public enum CategoryType {
    INCOME,
    EXPENSE,
    INITIAL_BALANCE,
    ADJUSTMENT,
    LIABILITY_OPENING;
    public BigDecimal signed(BigDecimal amount) {
        return this == EXPENSE || this == LIABILITY_OPENING ? amount.negate() : amount;
    }
    public boolean isOpeningBalance() {
        return this == INITIAL_BALANCE || this == LIABILITY_OPENING;
    }
}