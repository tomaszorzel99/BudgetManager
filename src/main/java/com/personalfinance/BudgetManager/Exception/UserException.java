package com.personalfinance.BudgetManager.Exception;

public class UserException extends RuntimeException {
    public UserException(Long id) {
        super("User not found with id: " + id);
    }
    public UserException(String email){
        super("User not found with email: " + email);
    }
}
