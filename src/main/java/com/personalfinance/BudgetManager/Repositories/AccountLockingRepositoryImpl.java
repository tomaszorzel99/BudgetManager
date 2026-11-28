package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.Account;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import java.util.Optional;

/**
 * Spring Data dołącza tę klasę do {@link AccountRepository} po nazwie (interfejs + „Impl”).
 *
 * Zwykłe zapytanie z {@code @Lock(PESSIMISTIC_WRITE)} blokuje wiersz, ale gdy konto jest już
 * w kontekście utrwalania, Hibernate zwraca obiekt z pamięci bez odświeżenia. Saldo mogło
 * się w tym czasie zmienić w innej transakcji. Dlatego blokujemy przez {@code refresh}.
 */
public class AccountLockingRepositoryImpl implements AccountLockingRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Account> findByIdWithLock(Long id) {
        Account account = entityManager.find(Account.class, id);
        if (account == null) {
            return Optional.empty();
        }
        // Niezapisane zmiany z tej transakcji trafiają najpierw do bazy, żeby refresh ich nie zgubił.
        entityManager.flush();
        entityManager.refresh(account, LockModeType.PESSIMISTIC_WRITE);
        return Optional.of(account);
    }
}