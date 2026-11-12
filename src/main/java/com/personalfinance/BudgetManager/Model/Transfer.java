package com.personalfinance.BudgetManager.Model;

import com.personalfinance.BudgetManager.Audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "transfers")
public class Transfer extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;

    @ManyToOne
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account toAccount;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "received_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal receivedAmount;

    @Column(name = "exchange_rate", nullable = false, precision = 30, scale = 12)
    private BigDecimal exchangeRate;

    private String description;

    @Column(nullable = false)
    private LocalDate transferDate;

    @ManyToOne
    private User user;
}
