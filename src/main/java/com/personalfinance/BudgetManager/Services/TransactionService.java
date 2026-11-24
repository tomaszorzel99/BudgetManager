package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.CreateTransactionRequest;
import com.personalfinance.BudgetManager.DTO.UpdateTransactionRequest;
import com.personalfinance.BudgetManager.Exception.*;
import com.personalfinance.BudgetManager.Model.*;
import com.personalfinance.BudgetManager.Repositories.*;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final AccountRepository accountRepository;
    private final UserService userService;

    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository, CategoryRepository categoryRepository, SubcategoryRepository subcategoryRepository, AccountRepository accountRepository, UserService userService) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.accountRepository = accountRepository;
        this.userService = userService;
    }

    @Transactional
    public Transaction createTransaction(CreateTransactionRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserException(userEmail));
        Account account = getAccessibleAccount(request.getAccountId(), user.getId());
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryException(request.getCategoryId()));
        Subcategory subcategory = findSubcategory(request.getSubcategoryId());
        validateCategory(category, subcategory, request.getType());
        if (request.getType() == CategoryType.INITIAL_BALANCE) {
            throw new IllegalArgumentException("Initial balance is created with the account");
        }
        requirePositive(request.getAmount());
        applyBalanceChange(account, request.getType(), request.getAmount());

        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : LocalDate.now());
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setSubcategory(subcategory);
        transaction.setAccount(account);

        return transactionRepository.save(transaction);
    }

    @Transactional
    public List<Transaction> getTransactions(
            Long groupId, String userEmail, Integer month, Integer year, CategoryType type, Long categoryId) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserException(userEmail));
        Long userId = user.getId();

        if (groupId != null && user.getUserGroups().stream()
                .noneMatch(g -> g.getId().equals(groupId))) {
            throw new AccessDeniedException("No access to this group");
        }

        Specification<Transaction> spec;


        if (groupId != null) {
            spec = TransactionSpecification.belongsToGroup(groupId);
        } else {
            spec = TransactionSpecification.hasUser(userId);
        }
        if (type != null) {
            spec = spec.and(TransactionSpecification.hasType(type));
        }
        if (categoryId != null) {
            spec = spec.and(TransactionSpecification.hasCategory(categoryId));
        }
        if (month != null && year != null) {
            spec = spec.and(TransactionSpecification.inMonth(month, year));
        }

        return transactionRepository.findAll(spec);
    }

    @Transactional
    public void deleteTransactionById(Long id) {

        Long userId = userService.getCurrentUser().getId();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AccessDeniedException("No access to this transaction"));

        if (transaction.getType() == CategoryType.INITIAL_BALANCE) {
            throw new IllegalArgumentException("Initial balance transaction cannot be deleted");
        }
        Account account = getAccessibleAccount(transaction.getAccount().getId(), userId);
        applyBalanceChange(account, transaction.getType(), transaction.getAmount().negate());
        transactionRepository.delete(transaction);
    }

    @Transactional
    public Transaction updateTransaction(Long id, UpdateTransactionRequest request) {
        Long userId = userService.getCurrentUser().getId();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AccessDeniedException("No access to this transaction"));
        if (transaction.getType() == CategoryType.INITIAL_BALANCE) {
            throw new IllegalArgumentException("Initial balance transaction cannot be edited");
        }
        Account account = getAccessibleAccount(transaction.getAccount().getId(), userId);
        Category category = request.getCategoryId() == null ? transaction.getCategory()
                : categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryException(request.getCategoryId()));
        boolean clearSubcategory = Boolean.TRUE.equals(request.getClearSubcategory());
        if (clearSubcategory && request.getSubcategoryId() != null) {
            throw new IllegalArgumentException("Pass either subcategoryId or clearSubcategory, not both");
        }
        Subcategory subcategory = resolveSubcategory(transaction.getSubcategory(), category,
                request.getSubcategoryId(), clearSubcategory);
        if (request.getCategoryId() != null || request.getSubcategoryId() != null) {
            validateCategory(category, subcategory, transaction.getType());
        }
        if (request.getAmount() != null) {
            requirePositive(request.getAmount());
            applyBalanceChange(account, transaction.getType(), request.getAmount().subtract(transaction.getAmount()));
        }
        updateExistingTransactionWithPatch(transaction, request);
        transaction.setCategory(category);
        transaction.setSubcategory(subcategory);
        return transactionRepository.save(transaction);
    }

    private Account getAccessibleAccount(Long accountId, Long userId) {
        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new AccountException(accountId));
        if (account.getDeletedAt() != null || account.getGroup().getUsers().stream()
                .noneMatch(member -> member.getId().equals(userId))) {
            throw new AccessDeniedException("No access to this account");
        }
        return account;
    }

    private Subcategory findSubcategory(Long subcategoryId) {
        if (subcategoryId == null) {
            return null;
        }
        return subcategoryRepository.findById(subcategoryId)
                .orElseThrow(() -> new SubcategoryException(subcategoryId));
    }

    /** Po zmianie kategorii stara podkategoria, która do niej nie pasuje, jest zdejmowana. */
    private Subcategory resolveSubcategory(Subcategory current, Category category, Long subcategoryId, boolean clear) {
        if (clear) {
            return null;
        }
        if (subcategoryId != null) {
            return findSubcategory(subcategoryId);
        }
        if (current != null && !current.getCategory().getId().equals(category.getId())) {
            return null;
        }
        return current;
    }

    private void validateCategory(Category category, Subcategory subcategory, CategoryType type) {
        if (category.getType() != type
                || (subcategory != null && !subcategory.getCategory().getId().equals(category.getId()))) {
            throw new IllegalArgumentException("Category, subcategory and transaction type must match");
        }
    }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private void applyBalanceChange(Account account, CategoryType type, BigDecimal amount) {
        BigDecimal delta = type == CategoryType.EXPENSE ? amount.negate() : amount;
        account.setBalance(account.getBalance().add(delta));
    }

    private void updateExistingTransactionWithPatch(Transaction existingTransaction, UpdateTransactionRequest request) {
        if (request.getAmount() != null) existingTransaction.setAmount(request.getAmount());
        if (request.getDescription() != null) existingTransaction.setDescription(request.getDescription());
        if (request.getTransactionDate() != null) existingTransaction.setTransactionDate(request.getTransactionDate());

    }

}
