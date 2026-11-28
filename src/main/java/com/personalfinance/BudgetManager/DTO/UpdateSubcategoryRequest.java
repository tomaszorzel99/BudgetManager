package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Zmiana częściowa: pola null zostają bez zmian. */
@Data
public class UpdateSubcategoryRequest {

    @Size(max = 255, message = "Subcategory name is too long")
    @Pattern(regexp = ".*\\S.*", message = "Subcategory name cannot be blank")
    private String name;

    @Size(max = 255, message = "Description is too long")
    private String description;

    private Long categoryId;
}