package com.personalfinance.BudgetManager.Model;

/** Klucze kategorii i podkategorii systemowych (kolumna system_key, V15). */
public final class SystemCategory {
    public static final String INITIAL_BALANCE = "INITIAL_BALANCE";
    public static final String ADJUSTMENT = "ADJUSTMENT";
    public static final String LOAN_COSTS = "LOAN_COSTS";
    public static final String LOAN_INTEREST = "LOAN_INTEREST";
    public static final String LOAN_FEES = "LOAN_FEES";
    /** Podkategoria korekty dla zmian wyceny nieruchomości (V16). */
    public static final String PROPERTY_VALUATION = "PROPERTY_VALUATION";
    /** Wbudowane podkategorie „Korekta salda” (V7, klucze od V19). */
    public static final String ADJUSTMENT_REFUND = "ADJUSTMENT_REFUND";
    public static final String ADJUSTMENT_MANUAL = "ADJUSTMENT_MANUAL";

    private SystemCategory() {
    }
}