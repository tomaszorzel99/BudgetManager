package com.personalfinance.BudgetManager.Exception;

public class SubcategoryException extends RuntimeException {
    public SubcategoryException(Long id) {
        super("Subcategory not found with id: " + id);
    }

    public SubcategoryException(String name) {
        super("Subcategory not found with name: " + name);
    }
}

