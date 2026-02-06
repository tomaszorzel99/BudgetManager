package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSubcategoryRequest {

    @NotBlank(message = "Subcategory name cannot be blank")
    private String name;
    private String description;
    @NotNull(message = "Category id cannot be null")
    private Long categoryId;
}
