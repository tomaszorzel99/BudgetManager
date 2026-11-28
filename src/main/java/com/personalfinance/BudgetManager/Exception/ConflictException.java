package com.personalfinance.BudgetManager.Exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}