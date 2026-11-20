package com.personalfinance.BudgetManager.Exception;

    public class TooManyLoginAttemptsException extends RuntimeException {
        public TooManyLoginAttemptsException() {
            super("Too many failed login attempts. Try again in 15 minutes.");
        }
}
