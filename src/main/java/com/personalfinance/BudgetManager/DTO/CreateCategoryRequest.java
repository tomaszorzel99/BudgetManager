package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateCategoryRequest {

    @NotBlank(message = "Category name cannot be blank")
    private String name;

    private String description;

    @NotBlank(message = "Category color cannot be blank")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a hex value like #6C47FF")
    private String color;

    @NotNull(message = "Category type cannot be null")
    private CategoryType type;
}