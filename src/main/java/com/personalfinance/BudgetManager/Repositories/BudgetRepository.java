package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @Query("SELECT b FROM Budget b JOIN FETCH b.category WHERE b.user.id = :userId")
    List<Budget> findAllForUser(@Param("userId") Long userId);

    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndCategoryIdAndCurrency(Long userId, Long categoryId, String currency);

    boolean existsByCategoryId(Long categoryId);
}
