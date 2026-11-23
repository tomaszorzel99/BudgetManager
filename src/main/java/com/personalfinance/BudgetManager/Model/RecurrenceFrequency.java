package com.personalfinance.BudgetManager.Model;

import java.time.LocalDate;

public enum RecurrenceFrequency {
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY;

    /**
     * Następny termin po {@code current}. Dla okresów miesięcznych dzień miesiąca
     * bierzemy z daty startu, żeby 31. nie „zjeżdżał” na 28. po lutym.
     */
    public LocalDate next(LocalDate current, LocalDate start) {
        return switch (this) {
            case WEEKLY -> current.plusWeeks(1);
            case BIWEEKLY -> current.plusWeeks(2);
            case MONTHLY -> anchored(current.plusMonths(1), start);
            case QUARTERLY -> anchored(current.plusMonths(3), start);
            case YEARLY -> anchored(current.plusYears(1), start);
        };
    }

    /** Przybliżona liczba wystąpień w miesiącu, do szacowania kosztu miesięcznego. */
    public double perMonth() {
        return switch (this) {
            case WEEKLY -> 52.0 / 12;
            case BIWEEKLY -> 26.0 / 12;
            case MONTHLY -> 1;
            case QUARTERLY -> 1.0 / 3;
            case YEARLY -> 1.0 / 12;
        };
    }

    private static LocalDate anchored(LocalDate date, LocalDate start) {
        int day = Math.min(start.getDayOfMonth(), date.lengthOfMonth());
        return date.withDayOfMonth(day);
    }
}
