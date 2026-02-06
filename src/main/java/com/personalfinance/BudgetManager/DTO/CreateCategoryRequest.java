package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.CategoryType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCategoryRequest {

    @NotBlank(message = "Category name cannot be blank")
    private String name;
    private String description;
    @NotBlank(message = "Category color cannot be blank")
    private String color;
    @NotNull(message = "Category type cannot be null")
    private CategoryType type;
}
