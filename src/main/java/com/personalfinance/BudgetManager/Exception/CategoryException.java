package com.personalfinance.BudgetManager.Exception;

public class CategoryException extends RuntimeException{
        public CategoryException(Long id) {
            super ("Category not found with id: " + id);
        }
    }
}
