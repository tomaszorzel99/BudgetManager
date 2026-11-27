package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.InstallmentType;
import com.personalfinance.BudgetManager.Model.LoanRateType;
import com.personalfinance.BudgetManager.Model.OverpaymentStrategy;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Nowy kredyt. {@code outstandingPrincipal} to kwota „pozostało do spłaty” z wyciągu na dzień
 * {@code balanceDate}; {@code originalPrincipal} i {@code startDate} pochodzą z umowy.
 * {@code subsidizedRate} i {@code subsidyEndDate} tylko przy kredycie z dopłatą (np. Bezpieczny Kredyt 2%).
 */
public record CreateLoanRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Long groupId,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}", message = "Currency must be a 3-letter code") String currency,
        @Size(max = 100) String lender,
        @NotNull @DecimalMin("0.01") @Digits(integer = 18, fraction = 2) BigDecimal originalPrincipal,
        @NotNull @DecimalMin("0.01") @Digits(integer = 18, fraction = 2) BigDecimal outstandingPrincipal,
        LocalDate balanceDate,
        @NotNull LocalDate startDate,
        @NotNull @Min(1) @Max(600) Integer termMonths,
        @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 4) BigDecimal annualInterestRate,
        @NotNull InstallmentType installmentType,
        LoanRateType rateType,
        @NotNull @Min(1) @Max(31) Integer paymentDay,
        OverpaymentStrategy overpaymentStrategy,
        Long defaultPaymentAccountId,
        @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 4) BigDecimal subsidizedRate,
        LocalDate subsidyEndDate) {
}