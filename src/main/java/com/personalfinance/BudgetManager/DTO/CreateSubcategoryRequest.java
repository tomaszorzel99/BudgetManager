package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSubcategoryRequest {

    @NotBlank(message = "Subcategory name cannot be blank")
    private String name;

    private String description;
    private Long categoryId;
}
