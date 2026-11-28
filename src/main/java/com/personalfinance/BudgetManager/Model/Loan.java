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
 * Parametry kredytu przypięte do konta typu LOAN. Saldo długu to saldo konta (ujemne);
 * tu trzymamy tylko dane z umowy potrzebne do postępu, podpowiedzi rat i harmonogramu.
 */
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

    /**
     * Oprocentowanie roczne w procentach, np. 7.2500 = 7,25%.
     */
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

    /**
     * Domyślna strategia nadpłaty; przy każdej nadpłacie można wybrać inną.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "overpayment_strategy", nullable = false, length = 20)
    private OverpaymentStrategy overpaymentStrategy = OverpaymentStrategy.SHORTEN_TERM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_payment_account_id")
    private Account defaultPaymentAccount;

    /**
     * Dopłata do rat (np. Bezpieczny Kredyt 2%): oprocentowanie w procentach, które faktycznie płaci
     * kredytobiorca. Różnicę do {@link #annualInterestRate} pokrywa państwo. Null = kredyt bez dopłat.
     */
    @Column(name = "subsidized_rate", precision = 7, scale = 4)
    private BigDecimal subsidizedRate;

    /**
     * Ostatni dzień, za który przysługuje dopłata (włącznie). Null przy dopłacie = bez końca.
     */
    @Column(name = "subsidy_end_date")
    private LocalDate subsidyEndDate;

    /**
     * Czy w danym dniu kredyt ma dopłatę do odsetek.
     */
    public boolean hasSubsidyOn(LocalDate date) {
        return subsidizedRate != null
                && subsidizedRate.compareTo(annualInterestRate) < 0
                && (subsidyEndDate == null || !date.isAfter(subsidyEndDate));
    }

    /**
     * Planowana data ostatniej raty według umowy (bez uwzględnienia nadpłat).
     */
    public LocalDate plannedEndDate() {
        return startDate.plusMonths(termMonths);
    }

    /** Najbliższy termin raty w dniu {@code from} lub później; 31. w krótszym miesiącu to jego ostatni dzień. */
    public LocalDate nextPaymentDate(LocalDate from) {
        LocalDate candidate = from.withDayOfMonth(Math.min(paymentDay, from.lengthOfMonth()));
        if (candidate.isBefore(from)) {
            LocalDate next = from.plusMonths(1);
            candidate = next.withDayOfMonth(Math.min(paymentDay, next.lengthOfMonth()));
        }
        return candidate;
    }

    /**
     * Ostatni termin raty przed dniem {@code date} (bez tego dnia), nie wcześniej niż start kredytu.
     * Od niego biegną odsetki, gdy w aplikacji nie ma jeszcze żadnej spłaty: nadpłata 7.11 przy racie
     * płatnej 5. liczy odsetki od 5.11, a rata 5.12 od 5.11.
     */
    public LocalDate lastDueDateBefore(LocalDate date) {
        LocalDate due = date.withDayOfMonth(Math.min(paymentDay, date.lengthOfMonth()));
        if (!due.isBefore(date)) {
            LocalDate previous = date.minusMonths(1);
            due = previous.withDayOfMonth(Math.min(paymentDay, previous.lengthOfMonth()));
        }
        return due.isBefore(startDate) ? startDate : due;
    }
}