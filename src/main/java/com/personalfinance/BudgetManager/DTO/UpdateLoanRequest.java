package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.LoanRateType;
import com.personalfinance.BudgetManager.Model.OverpaymentStrategy;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Zmiana parametrów kredytu. Saldo długu zmienia się tylko przez spłaty i korekty. Pola null zostają bez zmian. */
public record UpdateLoanRequest(
        @Size(min = 1, max = 100) String name,
        @Size(max = 100) String lender,
        @Min(1) @Max(600) Integer termMonths,
        @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 4) BigDecimal annualInterestRate,
        LoanRateType rateType,
        @Min(1) @Max(31) Integer paymentDay,
        OverpaymentStrategy overpaymentStrategy,
        Long defaultPaymentAccountId,
        Boolean clearDefaultPaymentAccount,
        @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 4) BigDecimal subsidizedRate,
        LocalDate subsidyEndDate,
        /** true = kredyt bez dopłaty (czyści oba pola dopłaty). */
        Boolean clearSubsidy) {
}