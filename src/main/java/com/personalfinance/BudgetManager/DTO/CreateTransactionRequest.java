package com.personalfinance.BudgetManager.DTO;


import com.personalfinance.BudgetManager.Model.CategoryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateTransactionRequest {

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    @NotNull
    private CategoryType type;
    private String description;
    private Long accountId;
    private Long userId;
    private Long categoryId;
    private Long subcategoryId;
}
