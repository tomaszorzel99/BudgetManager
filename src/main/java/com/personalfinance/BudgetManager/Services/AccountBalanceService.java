package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.Exception.AccountException;
import com.personalfinance.BudgetManager.Model.Account;
import com.personalfinance.BudgetManager.Repositories.AccountRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class AccountBalanceService {

    private final AccountRepository accountRepository;

    public AccountBalanceService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount){
        Account fromAccount = accountRepository.findByIdWithLock(fromAccountId).orElseThrow(() -> new AccountException(fromAccountId));
        Account toAccount = accountRepository.findByIdWithLock(toAccountId).orElseThrow(() -> new AccountException(toAccountId));

        if(fromAccount.getBalance().compareTo(amount) < 0){
            throw new IllegalArgumentException("Not enough balance in the account");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        log.info("Transferred {} from account {} to account {}",
                amount, fromAccountId, toAccountId);
    }
}
