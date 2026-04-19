package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.Account;
import com.personalfinance.BudgetManager.Model.AccountType;
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

    @Query("SELECT a FROM Account a WHERE a.group IN :groups AND a.deletedAt IS NULL")
    List<Account> findActiveByGroupIn(Set<UserGroup> groups);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") Long id);

    Optional<Account> findByIdAndGroupUsersId(Long id, Long userId);


    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a " +
            "JOIN a.group ug " +
            "JOIN ug.users u " +
            "WHERE u.id = :userId " +
            "AND a.availableForSpending = true " +
            "AND a.deletedAt IS NULL"
            )
    BigDecimal getTotalAvailableBalance(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a " +
            "JOIN a.group ug " +
            "JOIN ug.users u " +
            "WHERE u.id = :userId " +
            "AND a.accountType = :accountType " +
            "AND a.deletedAt IS NULL")
    BigDecimal getTotalBalanceByType(
            @Param("userId") Long userId,
            @Param("accountType") AccountType type);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a " +
            "JOIN a.group ug " +
            "JOIN ug.users u " +
            "WHERE u.id = :userId " +
            "AND a.deletedAt IS NULL")
    BigDecimal getTotalBalance(@Param("userId") Long userId);

    @Query("SELECT a FROM Account a WHERE a.group IN :groups AND a.deletedAt IS NOT NULL")
    List<Account> findArchivedByGroupIn(Set<UserGroup> groups);
}
