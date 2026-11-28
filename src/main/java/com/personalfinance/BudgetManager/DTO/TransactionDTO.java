package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.CategoryType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TransactionDTO {
    private Long id;
    private BigDecimal amount;
    private CategoryType type;
    private String description;
    private LocalDate transactionDate;
    private LocalDateTime createdDate;
    private Long accountId;
    private String accountName;
    private String currency;
    private Long userId;
    private String userName;
    private Long categoryId;
    private Long subcategoryId;
    private String categoryName;
    private String subcategoryName;
}