package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.AccountType;
import com.personalfinance.BudgetManager.Model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    @Query("SELECT t FROM Transfer t WHERE t.user.id = :userId " +
            "AND (t.fromAccount.id = :accountId OR t.toAccount.id = :accountId) " +
            "AND t.transferDate BETWEEN :startDate AND :endDate")
    List<Transfer> findByUserAndAccountAndDateBetween(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Transfer t WHERE t.user.id = :userId " +
            "AND (t.fromAccount.id = :accountId OR t.toAccount.id = :accountId)")
    List<Transfer> findByUserAndAccount(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId);

    @Query("SELECT t FROM Transfer t WHERE t.user.id = :userId " +
            "AND t.transferDate BETWEEN :startDate AND :endDate")
    List<Transfer> findByUserAndDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    List<Transfer> findByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
            "WHERE t.user.id = :userId " +
            "AND t.toAccount.accountType = :accountType " +
            "AND YEAR(t.transferDate) = :year " +
            "AND MONTH(t.transferDate) = :month")
    BigDecimal getTotalTransfersToTypeByMonth(
            @Param("userId") Long userId,
            @Param("accountType") AccountType accountType,
            @Param("month") int month,
            @Param("year") int year);


    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
            "WHERE t.user.id = :userId " +
            "AND t.fromAccount.accountType = :accountType " +
            "AND YEAR(t.transferDate) = :year " +
            "AND MONTH(t.transferDate) = :month")
    BigDecimal getTotalTransfersFromTypeByMonth(
            @Param("userId") Long userId,
            @Param("accountType") AccountType accountType,
            @Param("month") int month,
            @Param("year") int year);


    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
            "WHERE t.user.id = :userId " +
            "AND t.toAccount.accountType = :accountType " +
            "AND t.transferDate <= :endDate")
    BigDecimal getCumulativeTransfersToType(
            @Param("userId") Long userId,
            @Param("accountType") AccountType accountType,
            @Param("endDate") LocalDate endDate);


    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
            "WHERE t.user.id = :userId " +
            "AND t.fromAccount.accountType = :accountType " +
            "AND t.transferDate <= :endDate")
    BigDecimal getCumulativeTransfersFromType(
            @Param("userId") Long userId,
            @Param("accountType") AccountType accountType,
            @Param("endDate") LocalDate endDate);
}
