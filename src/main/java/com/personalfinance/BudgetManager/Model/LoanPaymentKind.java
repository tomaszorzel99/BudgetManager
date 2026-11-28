package com.personalfinance.BudgetManager.Model;

public enum LoanPaymentKind {
    INSTALLMENT,
    OVERPAYMENT,
    DISBURSEMENT;

    public boolean isRepayment() {
        return this != DISBURSEMENT;
    }
}