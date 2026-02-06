package com.personalfinance.BudgetManager.Exception;

public class EmailException extends RuntimeException {
    public EmailException(String email) {
        super("Email already exists: " + email);
    }
}
