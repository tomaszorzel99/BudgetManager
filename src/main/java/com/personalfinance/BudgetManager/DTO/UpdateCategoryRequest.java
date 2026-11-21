package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.CategoryType;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateCategoryRequest {

    private String name;
    private String description;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a hex value like #6C47FF")
    private String color;

    private CategoryType categoryType;
}