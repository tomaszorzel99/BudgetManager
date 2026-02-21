package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.Account;
import com.personalfinance.BudgetManager.Model.AccountType;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Model.UserGroup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findAllByGroupIn(Set<UserGroup> groups);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") Long id);


    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a " +
            "JOIN a.group ug " +
            "JOIN ug.users u " +
            "WHERE u.id = :userId " +
            "AND a.availableForSpending = true")
    BigDecimal getTotalAvailableBalance(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a " +
            "JOIN a.group ug " +
            "JOIN ug.users u " +
            "WHERE u.id = :userId " +
            "AND a.accountType = :accountType")
    BigDecimal getTotalBalanceByType(
            @Param("userId") Long userId,
            @Param("type") AccountType type);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a " +
            "JOIN a.group ug " +
            "JOIN ug.users u " +
            "WHERE u.id = :userId")
    BigDecimal getTotalBalance(@Param("userId") Long userId);
}
