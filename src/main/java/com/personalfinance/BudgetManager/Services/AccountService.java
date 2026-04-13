package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.CreateAccountRequest;
import com.personalfinance.BudgetManager.DTO.UpdateAccountRequest;
import com.personalfinance.BudgetManager.Exception.AccountException;
import com.personalfinance.BudgetManager.Exception.UserException;
import com.personalfinance.BudgetManager.Model.*;
import com.personalfinance.BudgetManager.Repositories.*;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository, UserService userService, TransactionRepository transactionRepository, CategoryRepository categoryRepository, SubcategoryRepository subcategoryRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
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

        Account savedAccount = accountRepository.save(account);

        if (request.getInitialBalance() != null && request.getInitialBalance().compareTo(BigDecimal.ZERO) > 0) {
            createInitialTransaction(savedAccount, request.getInitialBalance());
        }
        return savedAccount;
    }

    public List<Account> getAccountsVisibleForUser(User user){
        Set<UserGroup> userGroups = user.getUserGroups();
        return accountRepository.findActiveByGroupIn(userGroups);
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

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0){
            throw new IllegalStateException("Cannot delete account with non-zero balance. Current balance: " +
                    account.getBalance());
        }
        accountRepository.delete(account);
    }

    @Transactional
    public void archiveAccount(Long id) throws AccessDeniedException {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountException(id));
        User currentUser = userService.getCurrentUser();

        if(!account.getGroup().getUsers().contains(currentUser)) {
            throw new AccessDeniedException("You are not owner of this account");
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0){
            throw new IllegalStateException("Cannot delete account with non-zero balance. Current balance: " +
                    account.getBalance());
        }

        account.setDeletedAt(LocalDateTime.now());
        accountRepository.save(account);

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
        Category category = categoryRepository.findAll().stream()
                .filter(c -> "Initial Balance".equals(c.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Category 'Initial Balance' not found"));

        Subcategory subcategory = subcategoryRepository.findByCategoryName("Initial Balance").stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Subcategory 'Initial Balance' not found"));

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setType(CategoryType.INITIAL_BALANCE);
        transaction.setDescription("Initial balance");
        transaction.setTransactionDate(LocalDate.now());
        transaction.setCategory(category);
        transaction.setSubcategory(subcategory);
        transaction.setAccount(account);
        transaction.setUser(account.getGroup().getUsers().iterator().next());
        transactionRepository.save(transaction);
    }
}
