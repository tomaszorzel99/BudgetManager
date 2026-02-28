package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.CreateAccountRequest;
import com.personalfinance.BudgetManager.DTO.UpdateAccountRequest;
import com.personalfinance.BudgetManager.Exception.AccountException;
import com.personalfinance.BudgetManager.Exception.UserException;
import com.personalfinance.BudgetManager.Model.*;
import com.personalfinance.BudgetManager.Repositories.AccountRepository;
import com.personalfinance.BudgetManager.Repositories.TransactionRepository;
import com.personalfinance.BudgetManager.Repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class AccountService {


    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository, UserService userService, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Account createAccount(CreateAccountRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException(email));

        UserGroup group = user.getUserGroups().stream()
                .filter(u -> u.getId().equals(request.getGroupId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User has no group"));

        Account account = new Account();
        account.setName(request.getName());
        account.setCurrency(request.getCurrency());
        account.setBalance(request.getInitialBalance());
        account.setGroup(group);
        account.setAccountType(request.getAccountType());
        account.setAvailableForSpending(request.isAvailableForSpending());

        if (request.getInitialBalance().compareTo(BigDecimal.ZERO) > 0) {
            createInitialTransaction(account, request.getInitialBalance());
        }
        return accountRepository.save(account);
    }



    public List<Account> getAccountsVisibleForUser(User user){
        Set<UserGroup> userGroups = user.getUserGroups();
        return accountRepository.findAllByGroupIn(userGroups);
    }



    public Account updateAccount(Long id, UpdateAccountRequest request, String email) throws AccessDeniedException {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountException(id));
        if(!account.getGroup().getUsers().stream().map(User::getEmail).toList().contains(email)){
            throw new AccessDeniedException("You are not owner of this account");
        }
        updateExistingAccountWithPatch(account, request);
        return accountRepository.save(account);
    }

    @Transactional
    public void deleteAccountById(Long id) throws AccessDeniedException {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountException(id));
        User currentUser = userService.getCurrentUser();

        if (!account.getGroup().getUsers().contains(currentUser)) {
            throw new AccessDeniedException("You are not owner of this account");
        }
        accountRepository.delete(account);
    }

    public void updateExistingAccountWithPatch(Account existingAccount, UpdateAccountRequest request){
        if (request.getName() != null) existingAccount.setName(request.getName());
        if (request.getAccountType() != null) existingAccount.setAccountType(request.getAccountType());
        if (request.getCurrency() != null) existingAccount.setCurrency(request.getCurrency());
        if (request.getBalance() != null) existingAccount.setBalance(request.getBalance());
        if (request.getAvailableForSpending() !=null) {
            existingAccount.setAvailableForSpending(request.getAvailableForSpending());
        }
    }

    private void createInitialTransaction(Account account, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setType(CategoryType.INITIAL_BALANCE);
        transaction.setDescription("Initial balance");
        transaction.setTransactionDate(LocalDate.now());
        transaction.setAccount(account);
        transaction.setUser(account.getGroup().getUsers().iterator().next());
        transactionRepository.save(transaction);
    }
}
