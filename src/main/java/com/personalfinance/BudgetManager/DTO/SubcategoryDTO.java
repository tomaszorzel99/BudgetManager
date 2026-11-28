package com.personalfinance.BudgetManager.DTO;

import lombok.Data;

@Data
public class SubcategoryDTO {
    private Long id;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    /** Grupa podkategorii; null dla podkategorii systemowej. */
    private Long groupId;
    /** Podkategoria systemowa: wspólna dla wszystkich, bez edycji i usuwania. */
    private boolean system;
}