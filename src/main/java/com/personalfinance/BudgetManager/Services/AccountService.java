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
    private final TransferRepository transferRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository, UserService userService, TransactionRepository transactionRepository, CategoryRepository categoryRepository, SubcategoryRepository subcategoryRepository, TransferRepository transferRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.transferRepository = transferRepository;
    }

    @Transactional
    public Account createAccount(CreateAccountRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(userId));

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

    public List<Account> getAccountsVisibleForUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(userId));
        return accountRepository.findActiveByGroupIn(user.getUserGroups());
    }

    public List<Account> getArchivedAccount(){
        User currentUser = userService.getCurrentUser();
        return accountRepository.findArchivedByGroupIn(currentUser.getUserGroups());
    }



    public Account updateAccount(Long id, UpdateAccountRequest request) throws AccessDeniedException {

        User currentUser = userService.getCurrentUser();
        Account account = accountRepository.findByIdAndGroupUsersId(id,  currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("You are not owner of this account or account does not exist"));
        updateExistingAccountWithPatch(account, request);
        return accountRepository.save(account);
    }

    @Transactional
    public void deleteAccountById(Long id, Long userId) throws AccessDeniedException {
        Account account = getOwnedAccount(id, userId);

        boolean hasRealTransaction = transactionRepository.existsByAccountIdAndTypeNot(id, CategoryType.INITIAL_BALANCE);
        if (hasRealTransaction) {
            throw new IllegalStateException("Cannot delete account with transactions. Please archive the account instead.");
        }
        transferRepository.deleteByFromAccountIdOrToAccountId(id, id);
        transactionRepository.deleteByAccountId(id);
        accountRepository.delete(account);
    }

    @Transactional
    public void archiveAccount(Long id, Long userId) throws AccessDeniedException {
        Account account = getOwnedAccount(id, userId);

        if (account.getDeletedAt() != null) {
            throw new IllegalStateException("Account is already archived");
        }

                if (account.getBalance().compareTo(BigDecimal.ZERO) != 0){
            throw new IllegalStateException("Cannot delete account with non-zero balance. Current balance: " +
                    account.getBalance());
        }

        account.setAvailableForSpending(false);
        account.setDeletedAt(LocalDateTime.now());
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

    private Account getOwnedAccount(Long id, Long userId) throws AccessDeniedException {
        return accountRepository.findByIdAndGroupUsersId(id, userId).
                orElseThrow(() -> new AccountException("Account not found or access denied"));
    }
}
