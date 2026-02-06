package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.CreateTransactionRequest;
import com.personalfinance.BudgetManager.Exception.*;
import com.personalfinance.BudgetManager.Model.*;
import com.personalfinance.BudgetManager.Repositories.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    public Transaction createTransaction(CreateTransactionRequest request){
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new AccountException(request.getAccountId()));
        User user = userRepository.findByEmail(request.getUserEmail())
                .orElseThrow(() -> new UserException(request.getUserEmail()));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryException(request.getCategoryId()));
        Subcategory subcategory = subcategoryRepository.findById(request.getSubcategoryId())
                .orElseThrow(() -> new SubcategoryException(request.getSubcategoryId()));

        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setSubcategory(subcategory);
        transaction.setAccount(account);


        if(CategoryType.INCOME.equals(transaction.getType())){
            account.setBalance(account.getBalance().add(transaction.getAmount()));
        } else if (CategoryType.EXPENSE.equals(transaction.getType())){
            account.setBalance(account.getBalance().subtract(transaction.getAmount()));
        }
        return transactionRepository.save(transaction);
    }

    public List<Transaction> getTransactions(
            String userEmail, Integer month, Integer year, CategoryType type, Long categoryId) {

        Long userId = null;
        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new UserException(userEmail));
            userId = user.getId();
        }

        LocalDate startDate = null;
        LocalDate endDate = null;
        if (month != null && year != null){
            startDate = LocalDate.of(year, month, 1);
            endDate = startDate.plusMonths(1).minusDays(1);
        }


        if (userId != null && month != null && year != null) {
            return transactionRepository.findByUserIdAndTransactionDateBetween(userId, startDate, endDate);
        }

        if (userId != null && type != null){
            return transactionRepository.findByUserIdAndType(userId, type);
        }

        if (userId != null & categoryId != null){
            return transactionRepository.findByUserIdAndCategoryId(userId, categoryId);
        }

        if (userId != null) {
            return transactionRepository.findByUserId(userId);
        }

        return transactionRepository.findAll();
    }

//    public List<Transaction> getTransactionsByUserEmailAndDateBetween(String userEmail, LocalDate startDate, LocalDate endDate){
//        User user = userRepository.findByEmail(userEmail)
//                .orElseThrow(() -> new UserException(userEmail));
//        return transactionRepository.findByUserIdAndTransactionDateBetween(user.getId(), startDate, endDate);
//    }
//
//    public List<Transaction> getTransactionsByUserIdAndType(Long userId, CategoryType type){
//        return transactionRepository.findByUserIdAndType(userId, type);
//    }
//
//    public List<Transaction> getTransactionsByUserIdAndCategoryId(Long userId, Long categoryId){
//        return transactionRepository.findByUserIdAndCategoryId(userId, categoryId);
//    }

    public void deleteTransactionById(Long id){
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionException(id));
        transactionRepository.delete(transaction);
    }
}
