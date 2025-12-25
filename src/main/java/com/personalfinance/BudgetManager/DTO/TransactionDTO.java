package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.CategoryType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionDTO {
    private Long id;
    private BigDecimal amount;
    private CategoryType type;
    private String description;
    private String notes;
    private LocalDate transactionDate;
    private String userName;
    private String categoryName;
    private String subcategoryName;
}
