package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.InstallmentType;
import com.personalfinance.BudgetManager.Model.LoanRateType;
import com.personalfinance.BudgetManager.Model.OverpaymentStrategy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


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
        BigDecimal installmentPrincipalPaid,
        BigDecimal overpaidPrincipal,
        BigDecimal interestPaid,
        BigDecimal subsidyReceived,
        BigDecimal feesPaid,
        long paymentsCount,
        LocalDate lastPaymentDate) {
}