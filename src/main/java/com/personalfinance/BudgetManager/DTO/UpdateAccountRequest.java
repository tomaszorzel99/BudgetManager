package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.AccountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** Zmiana częściowa: pola null zostają bez zmian. */
@Data
public class UpdateAccountRequest {

    @Size(max = 255, message = "Account name is too long")
    @Pattern(regexp = ".*\\S.*", message = "Account name cannot be blank")
    private String name;

    private AccountType accountType;

    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a 3-letter code")
    private String currency;

    @Min(value = 0, message = "Balance cannot be negative")
    private BigDecimal balance;

    private Boolean availableForSpending;
}