package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCategoryRequest {

    @NotBlank(message = "Category name cannot be blank")
    @Size(max = 255, message = "Category name is too long")
    private String name;

    @Size(max = 255, message = "Description is too long")
    private String description;

    @NotBlank(message = "Category color cannot be blank")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a hex value like #6C47FF")
    private String color;

    @NotNull(message = "Category type cannot be null")
    private CategoryType type;

    /** Grupa kategorii; można pominąć, gdy użytkownik należy do jednej grupy. */
    private Long groupId;
}