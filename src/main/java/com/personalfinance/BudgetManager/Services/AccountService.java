package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.Exception.AccountException;
import com.personalfinance.BudgetManager.Model.Account;
import com.personalfinance.BudgetManager.Repositories.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {


    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account createAccount(Account account) {
        return accountRepository.save(account);
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccountById(Long id) {
        return accountRepository.findById(id).orElseThrow(() -> new AccountException(id));
    }

    public List<Account> getAccountsByOwnerId(Long userId) {
        return accountRepository.findByOwnerId(userId);
    }

    public void deleteAccount(Long id) {
        accountRepository.deleteById(id);
    }

}
