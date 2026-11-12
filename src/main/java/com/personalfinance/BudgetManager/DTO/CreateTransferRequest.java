package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateTransferRequest {

    @NotNull
    private Long fromAccountId;

    @NotNull
    private Long toAccountId;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @Digits(integer = 18, fraction = 12)
    @DecimalMin("0.000000000001")
    private BigDecimal exchangeRate;

    private String description;

    private LocalDate transferDate;
}
