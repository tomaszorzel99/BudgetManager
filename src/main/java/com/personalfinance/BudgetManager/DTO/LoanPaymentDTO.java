package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.LoanPaymentKind;
import com.personalfinance.BudgetManager.Model.OverpaymentStrategy;

import java.math.BigDecimal;
import java.time.LocalDate;

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