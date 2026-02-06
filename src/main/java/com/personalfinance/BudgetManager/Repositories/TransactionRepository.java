package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.CategoryType;
import com.personalfinance.BudgetManager.Model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserId(Long userId);
    List<Transaction> findByUserIdAndTransactionDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    List<Transaction> findByUserIdAndType(Long userId, CategoryType type);
    List<Transaction> findByUserIdAndCategoryId(Long userId, Long categoryId);
//    Page<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId, Pageable pageable);

    @Query("SELECT SUM(t.amount) FROM Transaction t " +
    "WHERE t.user.id = :userId AND t.type = com.personalfinance.BudgetManager.Model.CategoryType.EXPENSE " +
    "AND MONTH(t.transactionDate) = :month AND YEAR(t.transactionDate) = :year")
    BigDecimal getTotalExpensesByMonthAndYear(
            @Param("userId") Long userId,
            @Param("month") int month,
            @Param("year") int year);

    @Query("SELECT SUM(t.amount) FROM Transaction t " +
            "WHERE t.user.id = :userId AND t.type = com.personalfinance.BudgetManager.Model.CategoryType.INCOME " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalIncomeByMonthAndYear(
            @Param("userId") Long userId,
            @Param("month") int month,
            @Param("year") int year);

    @Query("SELECT (COALESCE(SUM(CASE WHEN t.type = com.personalfinance.BudgetManager.Model.CategoryType.INCOME  THEN t.amount ELSE 0 END), 0) - " +
            "COALESCE(SUM(CASE WHEN t.type = com.personalfinance.BudgetManager.Model.CategoryType.EXPENSE THEN t.amount ELSE 0 END), 0)) " +
            "FROM Transaction t WHERE t.user.id = :userId " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getMonthlyBalance(
            @Param("userId") Long userId,
            @Param("month") int month,
            @Param("year") int year);
}
