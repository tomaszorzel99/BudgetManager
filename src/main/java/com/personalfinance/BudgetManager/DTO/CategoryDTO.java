package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.CategoryType;
import lombok.Data;

@Data
public class CategoryDTO {

    private Long id;
    private String name;
    private String description;
    private String color;
    private CategoryType type;
}
