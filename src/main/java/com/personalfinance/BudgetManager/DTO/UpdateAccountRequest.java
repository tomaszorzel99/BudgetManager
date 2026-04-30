package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.AccountType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateAccountRequest {

    private String name;
    private AccountType accountType;
    private String currency;

    @Min(value = 0, message = "Balance cannot be negative")
    private BigDecimal balance;
    private Boolean availableForSpending;
}
