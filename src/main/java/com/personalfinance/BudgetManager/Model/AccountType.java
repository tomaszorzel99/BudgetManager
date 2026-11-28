package com.personalfinance.BudgetManager.Model;

public enum AccountType {
    CASH,
    CHECKING,
    SAVINGS,
    INVESTMENT,
    LOAN,
    PROPERTY;

    public boolean isLiability() {
        return this == LOAN;
    }

    public boolean isProperty() {
        return this == PROPERTY;
    }

    public static boolean isProperty(AccountType type) {
        return type != null && type.isProperty();
    }

    public static boolean isLiability(AccountType type) {
        return type != null && type.isLiability();
    }
}