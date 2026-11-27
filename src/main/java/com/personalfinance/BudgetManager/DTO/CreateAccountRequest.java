package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.AccountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateAccountRequest {

    @NotBlank(message = "Account name cannot be blank")
    private String name;
    private AccountType accountType;
    private boolean availableForSpending;

    @NotNull
    private Long groupId;

    @NotBlank(message = "Currency cannot be blank")
    private String currency;

    @Min(value = 0, message = "Balance cannot be negative")
    private BigDecimal initialBalance;

    /** Dzień, na który podano saldo początkowe (domyślnie dziś). Np. dzień zakupu mieszkania albo dodania kredytu. */
    @PastOrPresent(message = "Initial balance date cannot be in the future")
    private LocalDate initialBalanceDate;

}