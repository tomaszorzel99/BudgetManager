package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.CreateAccountRequest;
import com.personalfinance.BudgetManager.Exception.AccountException;
import com.personalfinance.BudgetManager.Exception.UserException;
import com.personalfinance.BudgetManager.Model.Account;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Repositories.AccountRepository;
import com.personalfinance.BudgetManager.Repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {


    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public Account createAccount(CreateAccountRequest request) {
        User user = userRepository.findById(request.getUserId()).
                orElseThrow(() -> new UserException(request.getUserId()));

        Account account = new Account();
        account.setName(request.getName());
        account.setAccountNumber(request.getAccountNumber());
        account.setCurrency(request.getCurrency());
        account.setBalance(request.getBalance());
        account.setUser(user);
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
