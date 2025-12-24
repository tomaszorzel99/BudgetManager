package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Repositories.AccountRepository;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAccountRequest {

    @NotBlank(message = "Account name cannot be blank")
    private String name;

    private String accountNumber;

    @NotBlank(message = "Currence cannot be blank")
    private String currency;

    @Min(value = 0, message = "Balance cannot be negative")
    private BigDecimal balance;

    private Long userId;

}
