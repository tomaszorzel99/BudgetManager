package com.personalfinance.BudgetManager.Exception;

public class TransactionException extends RuntimeException{
    public TransactionException(Long id) {
        super ("Transaction not found with id: " + id);
    }
}
