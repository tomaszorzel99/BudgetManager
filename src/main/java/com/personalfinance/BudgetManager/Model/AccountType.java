package com.personalfinance.BudgetManager.Model;

public enum AccountType {
    CASH,
    CHECKING,
    SAVINGS,
    INVESTMENT,
    /** Kredyt: saldo ujemne (dług), nigdy dodatnie. */
    LOAN,
    /**
     * Nieruchomość (mieszkanie, dom, działka): aktywo z wartością szacunkową. Nie służy do płacenia,
     * wartość zmienia się aktualizacją wyceny. Równoważy kredyt hipoteczny w wartości netto.
     */
    PROPERTY;

    /** Konto zobowiązania: saldo ≤ 0, nie wlicza się do środków dostępnych do wydania. */
    public boolean isLiability() {
        return this == LOAN;
    }

    /** Majątek trwały: liczy się do wartości netto, ale nie do majątku płynnego ani do wydania. */
    public boolean isProperty() {
        return this == PROPERTY;
    }

    /** Konto, z którego się płaci i przelewa (nie kredyt i nie nieruchomość). */
    public boolean isLiquid() {
        return !isLiability() && !isProperty();
    }

    public static boolean isProperty(AccountType type) {
        return type != null && type.isProperty();
    }

    public static boolean isLiability(AccountType type) {
        return type != null && type.isLiability();
    }
}