package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.CategoryType;
import lombok.Data;

@Data
public class TransactionRequest {

    private int month;
    private int year;
    private CategoryType category;
    private Long categoryId;
}
