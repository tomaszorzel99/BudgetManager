package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.CategoryType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Zmiana częściowa: pola null zostają bez zmian. */
@Data
public class UpdateCategoryRequest {

    @Size(max = 255, message = "Category name is too long")
    @Pattern(regexp = ".*\\S.*", message = "Category name cannot be blank")
    private String name;

    @Size(max = 255, message = "Description is too long")
    private String description;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a hex value like #6C47FF")
    private String color;

    private CategoryType categoryType;
}