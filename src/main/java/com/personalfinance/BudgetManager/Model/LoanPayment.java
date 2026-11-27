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

/**
 * Jedna spłata (rata lub nadpłata) albo wypłata z kredytu. Trzyma kwoty z wyciągu i odnośniki
 * do operacji, które utworzyła: przelewu kapitału oraz wydatków na odsetki i opłaty.
 * Zmienia się wyłącznie przez {@code LoanPaymentService}, żeby części się nie rozjechały.
 */
@Entity
@Table(name = "loan_payments")
@Getter
@Setter
@NoArgsConstructor
public class LoanPayment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private LoanPaymentKind kind;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "principal_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal principalAmount = BigDecimal.ZERO;

    /** Odsetki naliczone przez bank, przed dopłatą. */
    @Column(name = "interest_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal interestAmount = BigDecimal.ZERO;

    /** Część odsetek pokryta dopłatą państwa; nie schodzi z konta i nie jest wydatkiem. */
    @Column(name = "subsidy_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal subsidyAmount = BigDecimal.ZERO;

    @Column(name = "fee_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    /** Strategia wybrana przy nadpłacie; null dla raty i wypłaty. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private OverpaymentStrategy strategy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_account_id", nullable = false)
    private Account payerAccount;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id", unique = true)
    private Transfer transfer;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interest_transaction_id", unique = true)
    private Transaction interestTransaction;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_transaction_id", unique = true)
    private Transaction feeTransaction;

    @Column(length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Odsetki, które faktycznie płaci kredytobiorca (po dopłacie). */
    public BigDecimal interestPaid() {
        return interestAmount.subtract(subsidyAmount);
    }

    /** Ile zeszło z konta płatnika (spłata) albo na nie wpłynęło po potrąceniu opłat (wypłata). */
    public BigDecimal payerAmount() {
        return kind.isRepayment()
                ? principalAmount.add(interestPaid()).add(feeAmount)
                : principalAmount.subtract(feeAmount);
    }
}