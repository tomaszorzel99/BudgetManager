package com.personalfinance.BudgetManager.Exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(Long accountId, BigDecimal required, BigDecimal available) {
        super("Insufficient funds on account id: " + accountId
                + ". Required: " + required
                + ", available: " + available);
    }
}