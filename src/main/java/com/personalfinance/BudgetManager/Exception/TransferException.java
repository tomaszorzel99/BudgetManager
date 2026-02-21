package com.personalfinance.BudgetManager.Exception;

public class TransferException extends RuntimeException {
    public TransferException(Long id) {
        super("Transfer not found with id: " + id);
    }
}
