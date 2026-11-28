package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.CategoryType;
import lombok.Data;

@Data
public class CategoryDTO {

    private Long id;
    private String name;
    private String description;
    private String color;
    private CategoryType type;
    /** Grupa kategorii; null dla kategorii systemowej. */
    private Long groupId;
    /** Kategoria systemowa: wspólna dla wszystkich, bez edycji i usuwania. */
    private boolean system;
}