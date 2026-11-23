package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    @Query("SELECT g FROM SavingsGoal g LEFT JOIN FETCH g.account WHERE g.user.id = :userId ORDER BY g.createdAt")
    List<SavingsGoal> findAllForUser(@Param("userId") Long userId);

    Optional<SavingsGoal> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("UPDATE SavingsGoal g SET g.account = null WHERE g.account.id = :accountId")
    int unlinkAccount(@Param("accountId") Long accountId);
}
