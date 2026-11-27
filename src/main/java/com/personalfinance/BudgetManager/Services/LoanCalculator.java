package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.Model.InstallmentType;
import com.personalfinance.BudgetManager.Model.LoanPaymentKind;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Podpowiedź podziału raty, bez Springa i bazy. Odsetki liczone za faktyczną liczbę dni
 * od ostatniej spłaty do dnia pobrania (dni / 365), więc rata pobrana po weekendzie wychodzi
 * wyższa, a nadpłata w środku miesiąca zaczyna nowy okres odsetkowy. Bank jest źródłem prawdy:
 * wynik to wartości startowe formularza, które użytkownik poprawia na kwoty z wyciągu.
 */
public final class LoanCalculator {

    private static final BigDecimal DAYS_IN_YEAR_PERCENT = new BigDecimal("36500");
    private static final BigDecimal TWELVE_HUNDRED = new BigDecimal("1200");
    private static final MathContext MC = MathContext.DECIMAL64;

    private LoanCalculator() {
    }

    /**
     * Dane wejściowe podpowiedzi.
     *
     * @param kind                 rodzaj operacji
     * @param date                 dzień pobrania
     * @param outstanding          kapitał do spłaty w dniu {@code interestFrom} (dodatni)
     * @param interestFrom         dzień ostatniej spłaty (od niego biegną odsetki)
     * @param annualRate           oprocentowanie z umowy w procentach
     * @param subsidizedRate       oprocentowanie płacone po dopłacie albo null, gdy dopłata nie przysługuje
     * @param installmentType      rata malejąca albo równa
     * @param plannedEndDate       dzień ostatniej raty według umowy
     * @param previousPrincipal    część kapitałowa ostatniej raty, gdy nadal obowiązuje (null = policz od nowa)
     */
    public record Input(LoanPaymentKind kind, LocalDate date, BigDecimal outstanding, LocalDate interestFrom,
                        BigDecimal annualRate, BigDecimal subsidizedRate, InstallmentType installmentType,
                        LocalDate plannedEndDate, BigDecimal previousPrincipal) {
    }

    /** Wynik: kwoty zaokrąglone do grosza oraz dane, z których powstały (do pokazania w oknie). */
    public record Suggestion(BigDecimal principal, BigDecimal interest, BigDecimal subsidy, BigDecimal fee,
                             LocalDate interestFrom, long interestDays, int remainingInstallments) {

        public BigDecimal payerAmount() {
            return principal.add(interest).subtract(subsidy).add(fee);
        }
    }

    public static Suggestion suggest(Input in) {
        if (in.kind() == LoanPaymentKind.DISBURSEMENT || in.outstanding().signum() <= 0) {
            return new Suggestion(zero(), zero(), zero(), zero(), in.interestFrom(), 0,
                    remainingInstallments(in.date(), in.plannedEndDate()));
        }
        long days = Math.max(0, ChronoUnit.DAYS.between(in.interestFrom(), in.date()));
        BigDecimal interest = interest(in.outstanding(), in.annualRate(), days);
        BigDecimal subsidy = zero();
        if (in.subsidizedRate() != null && in.subsidizedRate().compareTo(in.annualRate()) < 0) {
            BigDecimal paid = interest(in.outstanding(), in.subsidizedRate(), days);
            subsidy = interest.subtract(paid).max(BigDecimal.ZERO);
        }
        int remaining = remainingInstallments(in.date(), in.plannedEndDate());
        BigDecimal principal = in.kind() == LoanPaymentKind.INSTALLMENT
                ? installmentPrincipal(in, remaining)
                : BigDecimal.ZERO;
        return new Suggestion(money(principal), interest, subsidy, zero(), in.interestFrom(), days, remaining);
    }

    /** Odsetki za {@code days} dni: kapitał × stopa × dni / 365, do grosza. */
    public static BigDecimal interest(BigDecimal outstanding, BigDecimal annualRatePercent, long days) {
        return outstanding.multiply(annualRatePercent).multiply(BigDecimal.valueOf(days))
                .divide(DAYS_IN_YEAR_PERCENT, 2, RoundingMode.HALF_UP);
    }

    /** Liczba rat od miesiąca {@code date} do miesiąca ostatniej raty, obie włącznie; co najmniej 1. */
    public static int remainingInstallments(LocalDate date, LocalDate plannedEndDate) {
        long months = ChronoUnit.MONTHS.between(YearMonth.from(date), YearMonth.from(plannedEndDate)) + 1;
        return (int) Math.max(1, months);
    }

    private static BigDecimal installmentPrincipal(Input in, int remaining) {
        BigDecimal outstanding = in.outstanding();
        BigDecimal principal;
        if (in.installmentType() == InstallmentType.ANNUITY) {
            principal = annuityPrincipal(outstanding, in.annualRate(), remaining);
        } else if (in.previousPrincipal() != null && in.previousPrincipal().signum() > 0) {
            // Rata malejąca: część kapitałowa stała, dopóki nadpłata nie przeliczy jej na niższą.
            principal = in.previousPrincipal();
        } else {
            principal = outstanding.divide(BigDecimal.valueOf(remaining), 2, RoundingMode.HALF_UP);
        }
        // Ostatnia rata (albo ostatnia po nadpłatach) wyrównuje do końca długu.
        return remaining == 1 ? outstanding : principal.min(outstanding);
    }

    /** Rata równa: A = S·q / (1 − (1+q)^−n), kapitał = A − S·q; q to stopa miesięczna. */
    private static BigDecimal annuityPrincipal(BigDecimal outstanding, BigDecimal annualRate, int n) {
        if (annualRate.signum() == 0) {
            return outstanding.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        }
        BigDecimal q = annualRate.divide(TWELVE_HUNDRED, MC);
        BigDecimal factor = BigDecimal.ONE.add(q).pow(n, MC);
        BigDecimal payment = outstanding.multiply(q).multiply(factor)
                .divide(factor.subtract(BigDecimal.ONE), MC);
        return payment.subtract(outstanding.multiply(q)).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2);
    }
}