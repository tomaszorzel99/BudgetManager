package com.personalfinance.BudgetManager.DTO;


import com.personalfinance.BudgetManager.Model.CategoryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

    private LocalDate transactionDate;
    private String description;

    @NotNull(message = "Account id cannot be null")
    private Long accountId;

    @NotNull(message = "Category id cannot be null")
    private Long categoryId;

    @NotNull(message = "Subcategory id cannot be null")
    private Long subcategoryId;
}
