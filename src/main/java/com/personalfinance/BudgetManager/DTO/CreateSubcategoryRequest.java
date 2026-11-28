package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSubcategoryRequest {

    @NotBlank(message = "Subcategory name cannot be blank")
    @Size(max = 255, message = "Subcategory name is too long")
    private String name;

    @Size(max = 255, message = "Description is too long")
    private String description;

    @NotNull(message = "Category id cannot be null")
    private Long categoryId;
    private Long groupId;
}