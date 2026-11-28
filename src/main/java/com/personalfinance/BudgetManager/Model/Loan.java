package com.personalfinance.BudgetManager.Model;

import com.personalfinance.BudgetManager.Audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
public class Loan extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(length = 100)
    private String lender;

    @Column(name = "original_principal", nullable = false, precision = 38, scale = 2)
    private BigDecimal originalPrincipal;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Column(name = "annual_interest_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal annualInterestRate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "rate_type", nullable = false, length = 20)
    private LoanRateType rateType = LoanRateType.FIXED;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "installment_type", nullable = false, length = 20)
    private InstallmentType installmentType;

    @Column(name = "payment_day", nullable = false)
    private int paymentDay;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "overpayment_strategy", nullable = false, length = 20)
    private OverpaymentStrategy overpaymentStrategy = OverpaymentStrategy.SHORTEN_TERM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_payment_account_id")
    private Account defaultPaymentAccount;

    @Column(name = "subsidized_rate", precision = 7, scale = 4)
    private BigDecimal subsidizedRate;

    @Column(name = "subsidy_end_date")
    private LocalDate subsidyEndDate;

    public boolean hasSubsidyOn(LocalDate date) {
        return subsidizedRate != null
                && subsidizedRate.compareTo(annualInterestRate) < 0
                && (subsidyEndDate == null || !date.isAfter(subsidyEndDate));
    }

    public LocalDate plannedEndDate() {
        return startDate.plusMonths(termMonths);
    }

    public LocalDate nextPaymentDate(LocalDate from) {
        LocalDate candidate = from.withDayOfMonth(Math.min(paymentDay, from.lengthOfMonth()));
        if (candidate.isBefore(from)) {
            LocalDate next = from.plusMonths(1);
            candidate = next.withDayOfMonth(Math.min(paymentDay, next.lengthOfMonth()));
        }
        return candidate;
    }

    public LocalDate lastDueDateBefore(LocalDate date) {
        LocalDate due = date.withDayOfMonth(Math.min(paymentDay, date.lengthOfMonth()));
        if (!due.isBefore(date)) {
            LocalDate previous = date.minusMonths(1);
            due = previous.withDayOfMonth(Math.min(paymentDay, previous.lengthOfMonth()));
        }
        return due.isBefore(startDate) ? startDate : due;
    }
}