package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Wyrównanie do wyciągu: {@code outstanding} to „pozostało do spłaty” według banku. */
public record ReconcileLoanRequest(
        @NotNull @DecimalMin("0") @Digits(integer = 18, fraction = 2) BigDecimal outstanding,
        LocalDate date,
        @Size(max = 255) String description) {
}