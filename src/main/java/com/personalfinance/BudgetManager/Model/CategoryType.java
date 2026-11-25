package com.personalfinance.BudgetManager.Model;

import java.math.BigDecimal;

public enum CategoryType {
    INCOME,
    EXPENSE,
    INITIAL_BALANCE,
    ADJUSTMENT,
    /** Saldo początkowe długu na koncie kredytu: kwota dodatnia, która zmniejsza saldo. */
    LIABILITY_OPENING;

    /** Kwota transakcji ze znakiem, z jakim zmienia saldo konta. */
    public BigDecimal signed(BigDecimal amount) {
        return this == EXPENSE || this == LIABILITY_OPENING ? amount.negate() : amount;
    }

    /** Saldo początkowe (zwykłe albo długu): tworzone razem z kontem, nie do edycji. */
    public boolean isOpeningBalance() {
        return this == INITIAL_BALANCE || this == LIABILITY_OPENING;
    }
}