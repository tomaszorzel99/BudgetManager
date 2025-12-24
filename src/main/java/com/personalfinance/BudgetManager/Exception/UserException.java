package com.personalfinance.BudgetManager.Exception;

public class UserException extends RuntimeException {
    public UserException(Long id) {
        super("Account not found with id: " + id);
    }
}
