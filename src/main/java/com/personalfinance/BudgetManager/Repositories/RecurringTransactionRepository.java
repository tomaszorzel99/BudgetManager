package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.Category;
import com.personalfinance.BudgetManager.Model.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    @Query("SELECT r FROM RecurringTransaction r JOIN FETCH r.account JOIN FETCH r.category " +
            "LEFT JOIN FETCH r.subcategory WHERE r.user.id = :userId ORDER BY r.active DESC, r.nextDate ASC")
    List<RecurringTransaction> findAllForUser(@Param("userId") Long userId);

    Optional<RecurringTransaction> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT r FROM RecurringTransaction r JOIN FETCH r.user " +
            "WHERE r.active = true AND r.autoPost = true AND r.nextDate <= :date")
    List<RecurringTransaction> findDueForAutoPost(@Param("date") LocalDate date);

    boolean existsByAccountId(Long accountId);
    boolean existsByCategoryId(Long categoryId);

    /** Przeniesienie podkategorii do innej kategorii: stałe wydatki z tą podkategorią idą razem z nią. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE RecurringTransaction r SET r.category = :category WHERE r.subcategory.id = :subcategoryId")
    int moveSubcategoryToCategory(@Param("subcategoryId") Long subcategoryId, @Param("category") Category category);

    @Modifying
    @Query("DELETE FROM RecurringTransaction r WHERE r.account.id = :accountId")
    int deleteAllForAccount(@Param("accountId") Long accountId);

    @Modifying
    @Query("UPDATE RecurringTransaction r SET r.active = false WHERE r.account.id = :accountId")
    int deactivateAllForAccount(@Param("accountId") Long accountId);
}
