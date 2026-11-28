package com.personalfinance.BudgetManager.DTO;

import lombok.Data;

@Data
public class SubcategoryDTO {
    private Long id;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    private Long groupId;
    private boolean system;
}