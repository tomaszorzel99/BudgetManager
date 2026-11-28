package com.personalfinance.BudgetManager.Exception;

/** Konto nie istnieje albo użytkownik go nie widzi: odpowiedź 404. */
public class AccountException extends RuntimeException {
    public AccountException(Long id) {
        super("Account not found with id: " + id);
    }

    public AccountException(String message) {
        super(message);
    }
}