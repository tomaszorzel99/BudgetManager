package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.CategoryType;
import com.personalfinance.BudgetManager.Model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByIdAndUserEmail(Long id, String email);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.type = :type " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalExpensesByMonth(
            @Param("userId") Long userId,
            @Param("type") CategoryType categoryType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.type = :type " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalIncomeByMonth(
            @Param("userId") Long userId,
            @Param("type") CategoryType categoryType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);


    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.type = :type " +
            "AND t.transactionDate <= :endDate")
    BigDecimal getCumulativeExpenses(
            @Param("userId") Long userId,
            @Param("type") CategoryType categoryType,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.type = :type " +
            "AND t.transactionDate <= :endDate")
    BigDecimal getCumulativeIncome(
            @Param("userId") Long userId,
            @Param("type") CategoryType categoryType,
            @Param("endDate") LocalDate endDate);


    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.type = :type " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalExpensesBetween(
            @Param("userId") Long userId,
            @Param("type") CategoryType categoryType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.type = :type " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalIncomeBetween(
            @Param("userId") Long userId,
            @Param("type") CategoryType categoryType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
