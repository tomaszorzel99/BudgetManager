package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.InstallmentType;
import com.personalfinance.BudgetManager.Model.LoanRateType;
import com.personalfinance.BudgetManager.Model.OverpaymentStrategy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Kredyt z podsumowaniem. Kwoty długu są dodatnie (saldo konta LOAN ze zmienionym znakiem). */
public record LoanDTO(
        Long id,
        Long accountId,
        String name,
        Long groupId,
        String groupName,
        String currency,
        String lender,
        BigDecimal originalPrincipal,
        BigDecimal outstandingPrincipal,
        BigDecimal repaidPrincipal,
        BigDecimal percentRepaid,
        LocalDate startDate,
        int termMonths,
        LocalDate plannedEndDate,
        BigDecimal annualInterestRate,
        LoanRateType rateType,
        InstallmentType installmentType,
        int paymentDay,
        LocalDate nextPaymentDate,
        OverpaymentStrategy overpaymentStrategy,
        Long defaultPaymentAccountId,
        String defaultPaymentAccountName,
        LocalDateTime createdAt,
        BigDecimal subsidizedRate,
        LocalDate subsidyEndDate,
        /** Suma spłaconego kapitału z rat zapisanych w aplikacji. */
        BigDecimal installmentPrincipalPaid,
        BigDecimal overpaidPrincipal,
        /** Odsetki zapłacone z własnej kieszeni (po dopłacie). */
        BigDecimal interestPaid,
        BigDecimal subsidyReceived,
        BigDecimal feesPaid,
        long paymentsCount,
        LocalDate lastPaymentDate) {
}