package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.LoanPaymentKind;
import com.personalfinance.BudgetManager.Model.OverpaymentStrategy;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Podpowiedź do formularza spłaty. To szacunek: odsetki za faktyczną liczbę dni od
 * {@code interestFrom} do {@code paymentDate}; kwoty z wyciągu mają pierwszeństwo.
 */
public record LoanPaymentSuggestionDTO(
        LoanPaymentKind kind,
        LocalDate paymentDate,
        BigDecimal principalAmount,
        BigDecimal interestAmount,
        BigDecimal subsidyAmount,
        BigDecimal feeAmount,
        BigDecimal payerAmount,
        LocalDate interestFrom,
        long interestDays,
        int remainingInstallments,
        BigDecimal outstandingBefore,
        Long payerAccountId,
        OverpaymentStrategy strategy) {
}