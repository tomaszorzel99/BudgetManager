package com.personalfinance.BudgetManager.Exception;


/**
 * Za mało środków na koncie: odpowiedź 422. Dziedziczy po IllegalArgumentException,
 * więc kod i testy, które łapią IllegalArgumentException, działają jak wcześniej.
 */
public class InsufficientFundsException extends IllegalArgumentException {
    public InsufficientFundsException(String accountName) {
        super("Not enough balance in account " + accountName);
    }
}