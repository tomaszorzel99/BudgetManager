package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.LoanPayment;
import com.personalfinance.BudgetManager.Model.LoanPaymentKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanPaymentRepository extends JpaRepository<LoanPayment, Long> {

    /** Spłaty kredytu od najnowszej; przy tej samej dacie późniejszy zapis pierwszy. */
    @Query("SELECT p FROM LoanPayment p JOIN FETCH p.payerAccount " +
            "WHERE p.loan.id = :loanId " +
            "AND (:from IS NULL OR p.paymentDate >= :from) AND (:to IS NULL OR p.paymentDate <= :to) " +
            "ORDER BY p.paymentDate DESC, p.id DESC")
    List<LoanPayment> findForLoan(@Param("loanId") Long loanId,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to);

    @Query("SELECT p FROM LoanPayment p WHERE p.id = :id AND p.loan.id = :loanId")
    Optional<LoanPayment> findByIdAndLoanId(@Param("id") Long id, @Param("loanId") Long loanId);

    /** Ostatnia spłata (rata lub nadpłata) przed podanym dniem; od niej liczą się odsetki. */
    @Query("SELECT p FROM LoanPayment p WHERE p.loan.id = :loanId " +
            "AND p.kind <> com.personalfinance.BudgetManager.Model.LoanPaymentKind.DISBURSEMENT " +
            "AND p.paymentDate < :before " +
            "ORDER BY p.paymentDate DESC, p.id DESC")
    List<LoanPayment> findRepaymentsBefore(@Param("loanId") Long loanId, @Param("before") LocalDate before);

    /** Suma kapitału danego rodzaju w okresie, po autorze operacji (jak pozostałe raporty). */
    @Query("SELECT COALESCE(SUM(p.principalAmount), 0) FROM LoanPayment p " +
            "WHERE p.user.id = :userId AND p.kind = :kind " +
            "AND p.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal sumPrincipal(@Param("userId") Long userId,
                            @Param("kind") LoanPaymentKind kind,
                            @Param("startDate") LocalDate startDate,
                            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(p.subsidyAmount), 0) FROM LoanPayment p " +
            "WHERE p.user.id = :userId AND p.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal sumSubsidy(@Param("userId") Long userId,
                          @Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate);

    /** Podsumowanie jednego kredytu: kapitał z rat, nadpłaty, wypłaty, odsetki, dopłaty, opłaty, liczba i daty spłat. */
    @Query("SELECT new com.personalfinance.BudgetManager.Repositories.LoanPaymentRepository$Totals(" +
            "COALESCE(SUM(CASE WHEN p.kind = com.personalfinance.BudgetManager.Model.LoanPaymentKind.INSTALLMENT THEN p.principalAmount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN p.kind = com.personalfinance.BudgetManager.Model.LoanPaymentKind.OVERPAYMENT THEN p.principalAmount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN p.kind = com.personalfinance.BudgetManager.Model.LoanPaymentKind.DISBURSEMENT THEN p.principalAmount ELSE 0 END), 0), " +
            "COALESCE(SUM(p.interestAmount), 0), COALESCE(SUM(p.subsidyAmount), 0), COALESCE(SUM(p.feeAmount), 0), " +
            "COUNT(p), MAX(p.paymentDate), " +
            "MAX(CASE WHEN p.kind = com.personalfinance.BudgetManager.Model.LoanPaymentKind.INSTALLMENT THEN p.paymentDate ELSE NULL END)) " +
            "FROM LoanPayment p WHERE p.loan.id = :loanId")
    Totals totalsForLoan(@Param("loanId") Long loanId);

    record Totals(BigDecimal installmentPrincipal, BigDecimal overpaidPrincipal, BigDecimal disbursed,
                  BigDecimal interest, BigDecimal subsidy, BigDecimal fees, long count, LocalDate lastPaymentDate,
                  LocalDate lastInstallmentDate) {
        public static final Totals EMPTY = new Totals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, null, null);
    }
}