package com.personalfinance.BudgetManager.Exception;



public class AccountException extends RuntimeException{
    public AccountException(Long id) {
        super ("Account not found with id: " + id);
    }
}
