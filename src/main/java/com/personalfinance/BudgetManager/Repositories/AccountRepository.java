package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.Account;
import com.personalfinance.BudgetManager.Model.AccountType;
import com.personalfinance.BudgetManager.Model.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long>, AccountLockingRepository {

    @Query("SELECT DISTINCT a FROM Account a JOIN FETCH a.group g LEFT JOIN FETCH g.users " +
            "WHERE g IN :groups AND a.deletedAt IS NULL")
    List<Account> findActiveByGroupIn(Set<UserGroup> groups);

    Optional<Account> findByIdAndGroupUsersId(Long id, Long userId);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a " +
            "WHERE a.group.id IN :groupIds " +
            "AND a.availableForSpending = true " +
            "AND a.accountType <> com.personalfinance.BudgetManager.Model.AccountType.LOAN " +
            "AND a.deletedAt IS NULL")
    BigDecimal getTotalAvailableBalance(@Param("groupIds") Collection<Long> groupIds);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a " +
            "WHERE a.group.id IN :groupIds " +
            "AND a.accountType = :accountType " +
            "AND a.deletedAt IS NULL")
    BigDecimal getTotalBalanceByType(
            @Param("groupIds") Collection<Long> groupIds,
            @Param("accountType") AccountType type);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a " +
            "WHERE a.group.id IN :groupIds " +
            "AND a.deletedAt IS NULL")
    BigDecimal getTotalBalance(@Param("groupIds") Collection<Long> groupIds);

    @Query("SELECT DISTINCT a FROM Account a JOIN FETCH a.group g LEFT JOIN FETCH g.users " +
            "WHERE g IN :groups AND a.deletedAt IS NOT NULL")
    List<Account> findArchivedByGroupIn(Set<UserGroup> groups);

    @Query("SELECT a FROM Account a WHERE a.group.id IN :groupIds")
    List<Account> findForHistoricalReport(@Param("groupIds") Collection<Long> groupIds);
}