package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.Category;
import com.personalfinance.BudgetManager.Model.CategoryType;
import com.personalfinance.BudgetManager.Model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction>,
        TransactionTotalsRepository {

    boolean existsByAccountIdAndTypeNotIn(Long id, Collection<CategoryType> types);
    boolean existsByAccountId(Long id);
    void deleteByAccountId(Long id);


    @Modifying(flushAutomatically = true)
    @Query("UPDATE Transaction t SET t.category = :category WHERE t.subcategory.id = :subcategoryId")
    int moveSubcategoryToCategory(@Param("subcategoryId") Long subcategoryId, @Param("category") Category category);


    @Query("SELECT COUNT(p) > 0 FROM LoanPayment p " +
            "WHERE p.interestTransaction.id = :transactionId OR p.feeTransaction.id = :transactionId")
    boolean isPartOfLoanPayment(@Param("transactionId") Long transactionId);

    @Override
    @EntityGraph(attributePaths = {"category", "subcategory", "account", "user"})
    List<Transaction> findAll(Specification<Transaction> spec, Sort sort);

    @Override
    @EntityGraph(attributePaths = {"category", "subcategory", "account", "user"})
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.account.group.id IN :groupIds " +
            "AND t.type = com.personalfinance.BudgetManager.Model.CategoryType.EXPENSE " +
            "AND t.category.systemKey = :systemKey " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalExpensesBySystemCategory(
            @Param("groupIds") Collection<Long> groupIds,
            @Param("systemKey") String systemKey,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.account.group.id IN :groupIds " +
            "AND t.type = :type " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumByTypeBetween(
            @Param("groupIds") Collection<Long> groupIds,
            @Param("type") CategoryType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.account.group.id IN :groupIds " +
            "AND t.type = :type " +
            "AND t.transactionDate <= :endDate")
    BigDecimal sumByTypeUntil(
            @Param("groupIds") Collection<Long> groupIds,
            @Param("type") CategoryType type,
            @Param("endDate") LocalDate endDate);


    @Query("SELECT t FROM Transaction t JOIN FETCH t.account a " +
            "WHERE a.id IN :accountIds AND t.transactionDate <= :endDate")
    List<Transaction> findForHistoricalReport(@Param("accountIds") List<Long> accountIds,
                                              @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.account a WHERE a.id IN :accountIds " +
            "AND t.type = com.personalfinance.BudgetManager.Model.CategoryType.LIABILITY_OPENING")
    List<Transaction> findLiabilityOpenings(@Param("accountIds") List<Long> accountIds);

    @Query("SELECT t.category.id, COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.account.group.id IN :groupIds " +
            "AND t.type = com.personalfinance.BudgetManager.Model.CategoryType.EXPENSE " +
            "AND UPPER(t.account.currency) = :currency " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY t.category.id")
    List<Object[]> sumExpensesByCategory(@Param("groupIds") Collection<Long> groupIds,
                                         @Param("currency") String currency,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.user JOIN FETCH t.category LEFT JOIN FETCH t.subcategory " +
            "WHERE t.account.id = :accountId")
    List<Transaction> findAllForAccountHistory(@Param("accountId") Long accountId);

    boolean existsByCategoryId(Long categoryId);
}
