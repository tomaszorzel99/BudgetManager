package com.personalfinance.BudgetManager.Exception;

/** Operacja niedozwolona w obecnym stanie danych (HTTP 409), np. edycja części spłaty kredytu. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}