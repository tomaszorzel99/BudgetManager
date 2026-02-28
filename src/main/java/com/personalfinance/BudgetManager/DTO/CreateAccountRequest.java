package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.AccountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

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

}
