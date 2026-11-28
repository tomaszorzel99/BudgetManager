package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.Account;

import java.util.Optional;

public interface AccountLockingRepository {

    Optional<Account> findByIdWithLock(Long id);
}