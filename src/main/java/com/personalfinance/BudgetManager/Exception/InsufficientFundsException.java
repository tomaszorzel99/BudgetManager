package com.personalfinance.BudgetManager.Exception;

public class InsufficientFundsException extends IllegalArgumentException {
    public InsufficientFundsException(String accountName) {
        super("Not enough balance in account " + accountName);
    }
}