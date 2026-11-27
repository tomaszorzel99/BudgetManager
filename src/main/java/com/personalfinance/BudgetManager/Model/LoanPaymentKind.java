package com.personalfinance.BudgetManager.Model;

/** Rodzaj operacji na kredycie. */
public enum LoanPaymentKind {
    /** Rata z harmonogramu: kapitał (może być 0 w karencji), odsetki, opłaty. */
    INSTALLMENT,
    /** Nadpłata: kapitał ponad harmonogram plus odsetki naliczone do dnia nadpłaty. */
    OVERPAYMENT,
    /** Wypłata transzy z kredytu na konto: dług rośnie. */
    DISBURSEMENT;

    /** Czy operacja zmniejsza dług (pieniądze idą z konta na kredyt). */
    public boolean isRepayment() {
        return this != DISBURSEMENT;
    }
}