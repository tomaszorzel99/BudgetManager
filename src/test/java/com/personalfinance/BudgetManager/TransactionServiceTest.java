package com.personalfinance.BudgetManager;

import com.personalfinance.BudgetManager.DTO.CreateTransactionRequest;
import com.personalfinance.BudgetManager.Model.*;
import com.personalfinance.BudgetManager.Repositories.*;
import com.personalfinance.BudgetManager.Services.TransactionService;
import com.personalfinance.BudgetManager.Services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock TransactionRepository transactionRepository;
    @Mock UserRepository userRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock SubcategoryRepository subcategoryRepository;
    @Mock AccountRepository accountRepository;
    @Mock UserService userService;
    @InjectMocks TransactionService service;

    @Test
    void cannotCreateTransactionOnAnotherGroupsAccount() {
        User user = new User();
        user.setId(1L);
        Account account = account(10L);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(accountRepository.findByIdWithLock(10L)).thenReturn(Optional.of(account));

        assertThrows(AccessDeniedException.class,
                () -> service.createTransaction(request(CategoryType.EXPENSE), "owner@example.com"));
        verify(transactionRepository, never()).save(any());
        assertEquals(0, account.getBalance().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void expenseChangesBalanceEvenWhenAccountIsNotAvailableForSpending() {
        User user = new User();
        user.setId(1L);
        Account account = account(10L);
        account.getGroup().getUsers().add(user);
        account.setAvailableForSpending(false);
        Category category = new Category();
        category.setId(20L);
        category.setType(CategoryType.EXPENSE);
        Subcategory subcategory = new Subcategory();
        subcategory.setCategory(category);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(accountRepository.findByIdWithLock(10L)).thenReturn(Optional.of(account));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));
        when(subcategoryRepository.findById(30L)).thenReturn(Optional.of(subcategory));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction transaction = service.createTransaction(request(CategoryType.EXPENSE), "owner@example.com");

        assertEquals(0, account.getBalance().compareTo(new BigDecimal("90.00")));
        assertNotNull(transaction.getTransactionDate());
    }

    @Test
    void deletingExpenseRestoresAccountBalance() {
        User user = new User();
        user.setId(1L);

        Account account = account(10L);
        account.setBalance(new BigDecimal("75.00"));
        account.getGroup().getUsers().add(user);

        Transaction expense = new Transaction();
        expense.setId(40L);
        expense.setAccount(account);
        expense.setType(CategoryType.EXPENSE);
        expense.setAmount(new BigDecimal("25.00"));

        when(userService.getCurrentUser()).thenReturn(user);
        when(transactionRepository.findByIdAndUserId(40L, 1L))
                .thenReturn(Optional.of(expense));
        when(accountRepository.findByIdWithLock(10L))
                .thenReturn(Optional.of(account));

        service.deleteTransactionById(40L);

        assertEquals(
                0,
                new BigDecimal("100.00").compareTo(account.getBalance())
        );
        verify(transactionRepository).delete(expense);
    }

    private Account account(Long id) {
        Account account = new Account();
        account.setId(id);
        account.setBalance(new BigDecimal("100.00"));
        account.setGroup(new UserGroup());
        return account;
    }

    private CreateTransactionRequest request(CategoryType type) {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAccountId(10L);
        request.setCategoryId(20L);
        request.setSubcategoryId(30L);
        request.setAmount(new BigDecimal("10.00"));
        request.setType(type);
        return request;
    }
}
