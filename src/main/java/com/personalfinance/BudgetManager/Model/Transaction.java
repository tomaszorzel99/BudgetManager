package com.personalfinance.BudgetManager.Model;

import com.personalfinance.BudgetManager.Audit.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "transactions", indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_transaction_date", columnList = "transaction_date"),
                @Index(name = "idx_user_date", columnList = "user_id, transaction_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Transaction extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Amount cannot be null")
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type;

    private String description;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    /** Opcjonalna: null = transakcja bez podkategorii (V13). */
    @JoinColumn(name = "subcategory_id")
    private Subcategory subcategory;

    /**
     * Kwota musi być dodatnia. Wyjątek: korekta może być ujemna (spadek wyceny nieruchomości, V16),
     * żeby obniżenie wartości mieszkania nie trafiało do wydatków.
     */
    @AssertTrue(message = "Amount must be greater than 0")
    public boolean isAmountValid() {
        if (amount == null || type == null) {
            return true; // braki zgłasza @NotNull
        }
        return amount.signum() > 0 || (type == CategoryType.ADJUSTMENT && amount.signum() < 0);
    }

    /** Kwota ze znakiem, z jakim zmienia saldo konta (wydatek i saldo początkowe długu na minus). */
    public BigDecimal signedAmount() {
        return type.signed(amount);
    }

}
