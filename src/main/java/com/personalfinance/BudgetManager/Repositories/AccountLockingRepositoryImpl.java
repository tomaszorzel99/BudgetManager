package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.Account;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import java.util.Optional;

public class AccountLockingRepositoryImpl implements AccountLockingRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Account> findByIdWithLock(Long id) {
        Account account = entityManager.find(Account.class, id);
        if (account == null) {
            return Optional.empty();
        }
        entityManager.flush();
        entityManager.refresh(account, LockModeType.PESSIMISTIC_WRITE);
        return Optional.of(account);
    }
}