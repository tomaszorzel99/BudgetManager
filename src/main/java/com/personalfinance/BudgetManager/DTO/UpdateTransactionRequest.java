package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateTransactionRequest {

    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    private String description;
    private LocalDate transactionDate;
    private Long categoryId;
    private Long subcategoryId;
}
