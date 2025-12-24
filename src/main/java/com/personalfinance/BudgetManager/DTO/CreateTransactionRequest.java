package com.personalfinance.BudgetManager.DTO;


import com.personalfinance.BudgetManager.Model.*;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @NotNull(message = "Date cannot be null")
    private LocalDate transactionDate;

    private Long userId;
    private Long categoryId;
    private Long subcategoryId;
}
