package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.LoanPaymentKind;
import com.personalfinance.BudgetManager.Model.OverpaymentStrategy;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Spłata z podziałem na części; {@code outstandingAfter} to dług zaraz po tej operacji. */
public record LoanPaymentDTO(
        Long id,
        Long loanId,
        LoanPaymentKind kind,
        LocalDate paymentDate,
        BigDecimal principalAmount,
        BigDecimal interestAmount,
        BigDecimal subsidyAmount,
        BigDecimal interestPaid,
        BigDecimal feeAmount,
        /** Ile zeszło z konta (spłata) albo na nie wpłynęło (wypłata). */
        BigDecimal payerAmount,
        OverpaymentStrategy strategy,
        Long payerAccountId,
        String payerAccountName,
        Long transferId,
        Long interestTransactionId,
        Long feeTransactionId,
        String description,
        BigDecimal outstandingAfter) {
}