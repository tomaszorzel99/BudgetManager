package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.AccountType;
import com.personalfinance.BudgetManager.Model.Transfer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    void deleteByFromAccountIdOrToAccountId(Long fromId, Long toId);
    boolean existsByFromAccountIdOrToAccountId(Long fromId, Long toId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transfer t WHERE t.id = :id")
    Optional<Transfer> findByIdWithLock(@Param("id") Long id);
    
    @Query("SELECT t FROM Transfer t JOIN FETCH t.fromAccount f JOIN FETCH t.toAccount a JOIN FETCH t.user " +
            "WHERE (f.group.id IN :groupIds OR a.group.id IN :groupIds) " +
            "AND (:userId IS NULL OR t.user.id = :userId) " +
            "AND (:accountId IS NULL OR f.id = :accountId OR a.id = :accountId) " +
            "AND (:startDate IS NULL OR t.transferDate >= :startDate) " +
            "AND (:endDate IS NULL OR t.transferDate <= :endDate) " +
            "ORDER BY t.transferDate DESC, t.id DESC")
    List<Transfer> findVisible(@Param("groupIds") Collection<Long> groupIds,
                               @Param("userId") Long userId,
                               @Param("accountId") Long accountId,
                               @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate);

//    @Query("SELECT t FROM Transfer t WHERE t.user.id = :userId " +
//            "AND (t.fromAccount.id = :accountId OR t.toAccount.id = :accountId) " +
//            "AND t.transferDate BETWEEN :startDate AND :endDate")
//    List<Transfer> findByUserAndAccountAndDateBetween(
//            @Param("userId") Long userId,
//            @Param("accountId") Long accountId,
//            @Param("startDate") LocalDate startDate,
//            @Param("endDate") LocalDate endDate);

//    @Query("SELECT t FROM Transfer t WHERE t.user.id = :userId " +
//            "AND (t.fromAccount.id = :accountId OR t.toAccount.id = :accountId)")
//    List<Transfer> findByUserAndAccount(
//            @Param("userId") Long userId,
//            @Param("accountId") Long accountId);

    /** Eksport „tylko moje”: przelewy autora w okresie. */
    @Query("SELECT t FROM Transfer t WHERE t.user.id = :userId " +
            "AND t.transferDate BETWEEN :startDate AND :endDate")
    List<Transfer> findByUserAndDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** Eksport „tylko moje”: wszystkie przelewy autora. */
    List<Transfer> findByUserId(Long userId);

    // Raporty: przelewy na konta (albo z kont) grup użytkownika, a nie tylko przelewy autora.

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
            "WHERE t.toAccount.group.id IN :groupIds " +
            "AND t.toAccount.accountType = :accountType " +
            "AND YEAR(t.transferDate) = :year " +
            "AND MONTH(t.transferDate) = :month")
    BigDecimal getTotalTransfersToTypeByMonth(
            @Param("groupIds") Collection<Long> groupIds,
            @Param("accountType") AccountType accountType,
            @Param("month") int month,
            @Param("year") int year);


    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
            "WHERE t.fromAccount.group.id IN :groupIds " +
            "AND t.fromAccount.accountType = :accountType " +
            "AND YEAR(t.transferDate) = :year " +
            "AND MONTH(t.transferDate) = :month")
    BigDecimal getTotalTransfersFromTypeByMonth(
            @Param("groupIds") Collection<Long> groupIds,
            @Param("accountType") AccountType accountType,
            @Param("month") int month,
            @Param("year") int year);


    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
            "WHERE t.toAccount.group.id IN :groupIds " +
            "AND t.toAccount.accountType = :accountType " +
            "AND t.transferDate <= :endDate")
    BigDecimal getCumulativeTransfersToType(
            @Param("groupIds") Collection<Long> groupIds,
            @Param("accountType") AccountType accountType,
            @Param("endDate") LocalDate endDate);


    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
            "WHERE t.fromAccount.group.id IN :groupIds " +
            "AND t.fromAccount.accountType = :accountType " +
            "AND t.transferDate <= :endDate")
    BigDecimal getCumulativeTransfersFromType(
            @Param("groupIds") Collection<Long> groupIds,
            @Param("accountType") AccountType accountType,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Transfer t JOIN FETCH t.fromAccount f JOIN FETCH t.toAccount a " +
            "WHERE (f.id IN :accountIds OR a.id IN :accountIds) " +
            "AND t.transferDate <= :endDate")
    List<Transfer> findForHistoricalReport(@Param("accountIds") List<Long> accountIds,
                                           @Param("endDate") LocalDate endDate);

//    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
//            "WHERE t.user.id = :userId AND t.toAccount.accountType = :accountType " +
//            "AND t.transferDate BETWEEN :startDate AND :endDate")
//    BigDecimal getTotalTransfersToTypeBetween(@Param("userId") Long userId,
//                                              @Param("accountType") AccountType accountType,
//                                              @Param("startDate") LocalDate startDate,
//                                              @Param("endDate") LocalDate endDate);

//    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
//            "WHERE t.user.id = :userId AND t.fromAccount.accountType = :accountType " +
//            "AND t.transferDate BETWEEN :startDate AND :endDate")
//    BigDecimal getTotalTransfersFromTypeBetween(@Param("userId") Long userId,
//                                                @Param("accountType") AccountType accountType,
//                                                @Param("startDate") LocalDate startDate,
//                                                @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Transfer t JOIN FETCH t.fromAccount JOIN FETCH t.toAccount " +
            "WHERE t.fromAccount.id = :accountId OR t.toAccount.id = :accountId")
    List<Transfer> findAllForAccountHistory(@Param("accountId") Long accountId);
}
