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
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceService accountBalanceService;
    private final UserService userService;

    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository, CategoryRepository categoryRepository, SubcategoryRepository subcategoryRepository, AccountRepository accountRepository, AccountBalanceService accountBalanceService, UserService userService) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.accountRepository = accountRepository;
        this.accountBalanceService = accountBalanceService;
        this.userService = userService;
    }

    @Transactional
    public Transaction createTransaction(CreateTransactionRequest request, String userEmail) {
        Account account = accountRepository.findByIdWithLock(request.getAccountId())
                .orElseThrow(() -> new AccountException(request.getAccountId()));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryException(request.getCategoryId()));
        Subcategory subcategory = subcategoryRepository.findById(request.getSubcategoryId())
                .orElseThrow(() -> new SubcategoryException(request.getSubcategoryId()));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserException(userEmail));

        CategoryType effectiveType = category.getType() == CategoryType.INITIAL_BALANCE
                || category.getType() == CategoryType.ADJUSTMENT
                ? category.getType()
                : request.getType();

        if (CategoryType.INCOME.equals(effectiveType)) {
            accountBalanceService.addIncome(request.getAccountId(), request.getAmount());
        } else if (CategoryType.EXPENSE.equals(effectiveType) && account.getAvailableForSpending()) {
            accountBalanceService.addExpense(request.getAccountId(), request.getAmount());
        } else if (CategoryType.ADJUSTMENT.equals(effectiveType)) {
            accountBalanceService.addIncome((request.getAccountId()), request.getAmount());
        }

        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setType(effectiveType);
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());
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

    public void deleteTransactionById(Long id) {

        Long userId = userService.getCurrentUser().getId();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AccessDeniedException("No access to this transaction"));

        Account account = transaction.getAccount();

        if (CategoryType.INCOME.equals(transaction.getType())) {
            account.setBalance(account.getBalance().subtract(transaction.getAmount()));
        } else if (CategoryType.EXPENSE.equals(transaction.getType())) {
            account.setBalance(account.getBalance().add(transaction.getAmount()));
        }

        accountRepository.save(account);
        transactionRepository.delete(transaction);
    }

    public Transaction updateTransaction(Long id, UpdateTransactionRequest request) {
        Long userId = userService.getCurrentUser().getId();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AccessDeniedException("No access to this transaction"));
        if (request.getAmount() != null && request.getAmount().compareTo(transaction.getAmount()) != 0) {
            BigDecimal diff = request.getAmount().subtract(transaction.getAmount());
            Account account = transaction.getAccount();
            if (transaction.getType() == CategoryType.INCOME) {
                account.setBalance(account.getBalance().add(diff));
            } else if (transaction.getType() == CategoryType.EXPENSE) {
                account.setBalance(account.getBalance().subtract(diff));
            }
            accountRepository.save(account);
            transaction.setAmount(request.getAmount());
        }

        updateExistingTransactionWithPatch(transaction, request);
        return transactionRepository.save(transaction);
    }

    private void updateExistingTransactionWithPatch(Transaction existingTransaction, UpdateTransactionRequest request) {
        if (request.getAmount() != null) existingTransaction.setAmount(request.getAmount());
        if (request.getDescription() != null) existingTransaction.setDescription(request.getDescription());
        if (request.getTransactionDate() != null) existingTransaction.setTransactionDate(request.getTransactionDate());
        if (request.getCategoryId() != null) {
            existingTransaction.setCategory(categoryRepository.findById(request.getCategoryId()).
                    orElseThrow(() -> new CategoryException(request.getCategoryId())));
        }
        if (request.getSubcategoryId() != null) {
            existingTransaction.setSubcategory(subcategoryRepository.findById(request.getSubcategoryId())
                    .orElseThrow(() -> new SubcategoryException(request.getSubcategoryId())));
        }
    }

}
