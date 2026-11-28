package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.AccountType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateAccountRequest {

    @NotBlank(message = "Account name cannot be blank")
    @Size(max = 255, message = "Account name is too long")
    private String name;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    private boolean availableForSpending;

    @NotNull
    private Long groupId;

    @NotBlank(message = "Currency cannot be blank")
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a 3-letter code")
    private String currency;

    @Min(value = 0, message = "Balance cannot be negative")
    @Digits(integer = 18, fraction = 2, message = "Amount can have at most 2 decimal places")
    private BigDecimal initialBalance;

    /** Dzień, na który podano saldo początkowe (domyślnie dziś). Np. dzień zakupu mieszkania albo dodania kredytu. */
    @PastOrPresent(message = "Initial balance date cannot be in the future")
    private LocalDate initialBalanceDate;

}