package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.CreateAccountRequest;
import com.personalfinance.BudgetManager.DTO.UpdateAccountRequest;
import com.personalfinance.BudgetManager.DTO.PropertyValueRequest;
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

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final TransferRepository transferRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final LoanRepository loanRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository, UserService userService, TransactionRepository transactionRepository, CategoryRepository categoryRepository, SubcategoryRepository subcategoryRepository, TransferRepository transferRepository, RecurringTransactionRepository recurringTransactionRepository, SavingsGoalRepository savingsGoalRepository, LoanRepository loanRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.transferRepository = transferRepository;
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.loanRepository = loanRepository;
    }

    @Transactional
    public Account createAccount(CreateAccountRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(userId));
        if (AccountType.isLiability(request.getAccountType())) {
            throw new IllegalArgumentException("Loan accounts are created via /api/loans");
        }

        UserGroup group = user.getUserGroups().stream()
                .filter(u -> u.getId().equals(request.getGroupId()))
                .findFirst()
                .orElseThrow(() -> new AccountException("User does not belong to the specified group"));

        Account account = new Account();
        account.setName(request.getName());
        account.setCurrency(request.getCurrency());
        account.setBalance(request.getInitialBalance());
        account.setGroup(group);
        account.setAccountType(request.getAccountType());
        // Mieszkania nie da się wydać: nieruchomość nigdy nie jest „dostępna do wydania”.
        account.setAvailableForSpending(request.isAvailableForSpending() && !AccountType.isProperty(request.getAccountType()));

        Account savedAccount = accountRepository.save(account);

        if (request.getInitialBalance() != null && request.getInitialBalance().compareTo(BigDecimal.ZERO) > 0) {
            LocalDate date = request.getInitialBalanceDate() != null ? request.getInitialBalanceDate() : LocalDate.now();
            createInitialTransaction(savedAccount, request.getInitialBalance(), date, user);
        }
        return savedAccount;
    }

    @Transactional
    public List<Account> getAccountsVisibleForUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(userId));
        return accountRepository.findActiveByGroupIn(user.getUserGroups());
    }

    @Transactional
    public List<Account> getArchivedAccount(){
        User currentUser = userService.getCurrentUser();
        return accountRepository.findArchivedByGroupIn(currentUser.getUserGroups());
    }


    @Transactional
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

        boolean hasRealTransaction = transactionRepository.existsByAccountIdAndTypeNotIn(id,
                List.of(CategoryType.INITIAL_BALANCE, CategoryType.LIABILITY_OPENING));
        if (hasRealTransaction) {
            throw new IllegalStateException("Cannot delete account with transactions. Please archive the account instead.");
        }
        if (loanRepository.hasPayments(id)) {
            throw new IllegalStateException("Cannot delete account with loan payments. Please archive the account instead.");
        }
        if (transferRepository.existsByFromAccountIdOrToAccountId(id, id)) {
            throw new IllegalStateException("Cannot delete account with transfers. Please archive the account instead.");
        }
        recurringTransactionRepository.deleteAllForAccount(id);
        savingsGoalRepository.unlinkAccount(id);
        loanRepository.unlinkPaymentAccount(id);
        loanRepository.deleteByAccountId(id);
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
        if (request.getAccountType() != null && request.getAccountType() != existingAccount.getAccountType()) {
            if (AccountType.isLiability(request.getAccountType()) || existingAccount.isLiability()) {
                throw new IllegalArgumentException("Cannot change an account to or from a loan; create a loan in the Loans section");
            }
            if (AccountType.isProperty(request.getAccountType()) || existingAccount.isProperty()) {
                // Np. mieszkanie wpisane wcześniej jako „Inwestycje”: można przenieść, jeśli konto nie ma
                // przychodów, wydatków ani przelewów (te nie mają sensu na nieruchomości).
                boolean hasCashFlow = transactionRepository.existsByAccountIdAndTypeNotIn(existingAccount.getId(),
                        List.of(CategoryType.INITIAL_BALANCE, CategoryType.LIABILITY_OPENING, CategoryType.ADJUSTMENT));
                if (hasCashFlow || transferRepository.existsByFromAccountIdOrToAccountId(existingAccount.getId(), existingAccount.getId())) {
                    throw new IllegalArgumentException("Only an account without income, expenses and transfers can be changed to or from a property");
                }
            }
            existingAccount.setAccountType(request.getAccountType());
            if (existingAccount.isProperty()) {
                existingAccount.setAvailableForSpending(false);
            }
        }
        //if (request.getCurrency() != null) existingAccount.setCurrency(request.getCurrency());
        if (request.getCurrency() != null
                && !request.getCurrency().equalsIgnoreCase(existingAccount.getCurrency())) {
            if (existingAccount.getBalance().signum() != 0
                    || transactionRepository.existsByAccountId(existingAccount.getId())
                    || transferRepository.existsByFromAccountIdOrToAccountId(existingAccount.getId(), existingAccount.getId())) {
                throw new IllegalArgumentException("Cannot change the currency of an account with a balance or history. Create another account and transfer funds with an exchange rate.");
            }
            existingAccount.setCurrency(request.getCurrency().toUpperCase(java.util.Locale.ROOT));
        }
        if (request.getBalance() != null) {
            throw new IllegalArgumentException("Balance cannot be edited directly; create an adjustment transaction");
        }
        if (Boolean.TRUE.equals(request.getAvailableForSpending()) && existingAccount.isLiability()) {
            throw new IllegalArgumentException("A loan account cannot be available for spending");
        }
        if (Boolean.TRUE.equals(request.getAvailableForSpending()) && existingAccount.isProperty()) {
            throw new IllegalArgumentException("A property cannot be available for spending");
        }
        if (request.getAvailableForSpending() !=null) {
            existingAccount.setAvailableForSpending(request.getAvailableForSpending());
        }
    }

    /**
     * Nowa wycena nieruchomości: różnica do obecnej wartości jako korekta „Wycena nieruchomości”.
     * Kwota korekty może być ujemna (spadek wartości), dzięki czemu nie trafia do wydatków.
     */
    @Transactional
    public Account revalueProperty(Long id, Long userId, PropertyValueRequest request) throws AccessDeniedException {
        Account account = accountRepository.findByIdWithLock(id)
                .filter(a -> a.getGroup().getUsers().stream().anyMatch(u -> u.getId().equals(userId)))
                .orElseThrow(() -> new AccessDeniedException("You are not owner of this account or account does not exist"));
        if (!account.isProperty()) {
            throw new IllegalArgumentException("Only a property account can be revalued");
        }
        if (account.getDeletedAt() != null) {
            throw new IllegalArgumentException("Account is archived");
        }
        BigDecimal value = request.value().setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal diff = value.subtract(account.getBalance());
        if (diff.signum() == 0) {
            return account;
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(userId));
        Category category = categoryRepository.findBySystemKey(SystemCategory.ADJUSTMENT)
                .orElseThrow(() -> new IllegalStateException("System category ADJUSTMENT is missing (migration V15)"));
        Transaction transaction = new Transaction();
        transaction.setAmount(diff);
        transaction.setType(CategoryType.ADJUSTMENT);
        transaction.setDescription(request.description() != null && !request.description().isBlank()
                ? request.description().trim() : "Aktualizacja wyceny");
        transaction.setTransactionDate(request.date() != null ? request.date() : LocalDate.now());
        transaction.setCategory(category);
        transaction.setSubcategory(subcategoryRepository.findBySystemKey(SystemCategory.PROPERTY_VALUATION).orElse(null));
        transaction.setAccount(account);
        transaction.setUser(user);
        transactionRepository.save(transaction);
        account.setBalance(value);
        return account;
    }

    private void createInitialTransaction(Account account, BigDecimal amount, LocalDate date, User creator) {
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
        transaction.setTransactionDate(date);
        transaction.setCategory(category);
        transaction.setSubcategory(subcategory);
        transaction.setAccount(account);
        transaction.setUser(creator);
        transactionRepository.save(transaction);
    }

    private Account getOwnedAccount(Long id, Long userId) throws AccessDeniedException {
        return accountRepository.findByIdAndGroupUsersId(id, userId).
                orElseThrow(() -> new AccountException("Account not found or access denied"));
    }
}
