package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.LoanPaymentKind;
import com.personalfinance.BudgetManager.Model.OverpaymentStrategy;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanPaymentRequest(
        @NotNull LoanPaymentKind kind,
        @NotNull LocalDate paymentDate,
        @NotNull Long payerAccountId,
        @DecimalMin("0") @Digits(integer = 18, fraction = 2) BigDecimal principalAmount,
        @DecimalMin("0") @Digits(integer = 18, fraction = 2) BigDecimal interestAmount,
        @DecimalMin("0") @Digits(integer = 18, fraction = 2) BigDecimal subsidyAmount,
        @DecimalMin("0") @Digits(integer = 18, fraction = 2) BigDecimal feeAmount,
        OverpaymentStrategy strategy,
        @Size(max = 255) String description) {
}