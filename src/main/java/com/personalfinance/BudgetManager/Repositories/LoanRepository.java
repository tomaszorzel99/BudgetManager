package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    /** Kredyty na niezarchiwizowanych kontach z grup, do których należy użytkownik. */
    @Query("SELECT DISTINCT l FROM Loan l JOIN FETCH l.account a JOIN FETCH a.group g JOIN g.users u " +
            "WHERE u.id = :userId AND a.deletedAt IS NULL ORDER BY l.id")
    List<Loan> findActiveForUser(@Param("userId") Long userId);

    @Query("SELECT l FROM Loan l JOIN FETCH l.account a JOIN FETCH a.group g JOIN g.users u " +
            "WHERE l.id = :id AND u.id = :userId")
    Optional<Loan> findByIdForUser(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Loan l WHERE l.account.id = :accountId")
    void deleteByAccountId(@Param("accountId") Long accountId);

    @Modifying
    @Query("UPDATE Loan l SET l.defaultPaymentAccount = NULL WHERE l.defaultPaymentAccount.id = :accountId")
    void unlinkPaymentAccount(@Param("accountId") Long accountId);
}