package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.CategoryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateCategoryRequest {

    private String name;
    private String description;
    private String color;
    private CategoryType categoryType;

}
