package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateAccountRequest {

    private String name;
    private String accountNumber;
    private String currency;

    @Min(value = 0, message = "Balance cannot be negative")
    private BigDecimal balance;
}
