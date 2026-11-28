package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.Category;
import com.personalfinance.BudgetManager.Model.CategoryType;
import com.personalfinance.BudgetManager.Model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByIdAndUserEmail(Long id, String email);
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);
    boolean existsByAccountIdAndTypeNot(Long id, CategoryType type);
    boolean existsByAccountIdAndTypeNotIn(Long id, Collection<CategoryType> types);
    boolean existsByAccountId(Long id);
    void deleteByAccountId(Long id);

    /** Przeniesienie podkategorii do innej kategorii: jej transakcje dostają nową kategorię. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Transaction t SET t.category = :category WHERE t.subcategory.id = :subcategoryId")
    int moveSubcategoryToCategory(@Param("subcategoryId") Long subcategoryId, @Param("category") Category category);

    /** Czy transakcja jest częścią spłaty kredytu (odsetki lub opłaty); takie zmienia się tylko przez spłatę. */
    @Query("SELECT COUNT(p) > 0 FROM LoanPayment p " +
            "WHERE p.interestTransaction.id = :transactionId OR p.feeTransaction.id = :transactionId")
    boolean isPartOfLoanPayment(@Param("transactionId") Long transactionId);

    /** Wydatki w kategorii systemowej (np. LOAN_COSTS) w okresie, tak jak inne raporty: po autorze. */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.type = com.personalfinance.BudgetManager.Model.CategoryType.EXPENSE " +
            "AND t.category.systemKey = :systemKey " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalExpensesBySystemCategory(
            @Param("userId") Long userId,
            @Param("systemKey") String systemKey,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);


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

    @Query("SELECT t FROM Transaction t JOIN FETCH t.account a " +
            "WHERE a.id IN :accountIds AND t.transactionDate <= :endDate")
    List<Transaction> findForHistoricalReport(@Param("accountIds") List<Long> accountIds,
                                              @Param("endDate") LocalDate endDate);

    /** Salda początkowe długu na podanych kontach kredytów (każdy kredyt ma jedno, z dniem „stan na dzień”). */
    @Query("SELECT t FROM Transaction t JOIN FETCH t.account a WHERE a.id IN :accountIds " +
            "AND t.type = com.personalfinance.BudgetManager.Model.CategoryType.LIABILITY_OPENING")
    List<Transaction> findLiabilityOpenings(@Param("accountIds") List<Long> accountIds);

    /** Suma wydatków użytkownika wg kategorii w okresie, tylko z kont w danej walucie. */
    @Query("SELECT t.category.id, COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.type = com.personalfinance.BudgetManager.Model.CategoryType.EXPENSE " +
            "AND UPPER(t.account.currency) = :currency " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY t.category.id")
    List<Object[]> sumExpensesByCategory(@Param("userId") Long userId,
                                         @Param("currency") String currency,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.user JOIN FETCH t.category LEFT JOIN FETCH t.subcategory " +
            "WHERE t.account.id = :accountId")
    List<Transaction> findAllForAccountHistory(@Param("accountId") Long accountId);

    boolean existsByCategoryId(Long categoryId);
}
