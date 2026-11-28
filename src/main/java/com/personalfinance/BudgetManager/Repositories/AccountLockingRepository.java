package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.Account;

import java.util.Optional;

/** Część {@link AccountRepository} pisana ręcznie: blokada konta z odświeżeniem stanu. */
public interface AccountLockingRepository {

    /**
     * Blokuje wiersz konta (SELECT ... FOR UPDATE) i zwraca jego aktualny stan z bazy.
     * Działa także wtedy, gdy konto było już wczytane w tej transakcji (np. przez transakcję
     * albo transfer), więc saldo nigdy nie pochodzi sprzed blokady.
     */
    Optional<Account> findByIdWithLock(Long id);
}