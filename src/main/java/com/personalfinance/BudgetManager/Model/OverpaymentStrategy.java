package com.personalfinance.BudgetManager.Model;

/** Co bank robi po nadpłacie: skraca okres albo obniża ratę. */
public enum OverpaymentStrategy {
    SHORTEN_TERM,
    REDUCE_INSTALLMENT
}