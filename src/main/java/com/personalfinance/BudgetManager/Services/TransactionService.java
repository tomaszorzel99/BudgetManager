package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.CreateTransactionRequest;
import com.personalfinance.BudgetManager.Exception.*;
import com.personalfinance.BudgetManager.Model.*;
import com.personalfinance.BudgetManager.Repositories.*;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository, CategoryRepository categoryRepository, SubcategoryRepository subcategoryRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Transaction createTransaction(CreateTransactionRequest request, String userEmail) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new AccountException(request.getAccountId()));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryException(request.getCategoryId()));
        Subcategory subcategory = subcategoryRepository.findById(request.getSubcategoryId())
                .orElseThrow(() -> new SubcategoryException(request.getSubcategoryId()));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserException(userEmail));

        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setSubcategory(subcategory);
        transaction.setAccount(account);


        if (CategoryType.INCOME.equals(transaction.getType())) {
            account.setBalance(account.getBalance().add(transaction.getAmount()));
        } else if (CategoryType.EXPENSE.equals(transaction.getType())) {
            account.setBalance(account.getBalance().subtract(transaction.getAmount()));
        }
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


        if(groupId != null) {
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
            spec = spec.and(TransactionSpecification.inMonth(year, month));
        }

        return transactionRepository.findAll(spec);
    }

    public void deleteTransactionById(Long id, String userEmail) {

        Transaction transaction = transactionRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new AccessDeniedException("No access to this transaction"));


        System.out.println("JWT email: " + userEmail);
        System.out.println("DB email: " + transaction.getUser().getEmail());
        transactionRepository.delete(transaction);
    }
}
