package com.personalfinance.BudgetManager.Model;

public enum AccountType {
    CASH,
    CHECKING,
    SAVINGS,
    INVESTMENT,
    /** Kredyt: saldo ujemne (dług), nigdy dodatnie. */
    LOAN;

    /** Konto zobowiązania: saldo ≤ 0, nie wlicza się do środków dostępnych do wydania. */
    public boolean isLiability() {
        return this == LOAN;
    }

    public static boolean isLiability(AccountType type) {
        return type != null && type.isLiability();
    }
}

