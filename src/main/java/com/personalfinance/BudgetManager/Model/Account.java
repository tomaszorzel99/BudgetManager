package com.personalfinance.BudgetManager.Model;


import com.personalfinance.BudgetManager.Audit.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
public class Account extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Account name cannot be blank")
    private String name;

    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    @NotBlank(message = "Currence cannot be blank")
    private String currency;

    private BigDecimal balance;

    private boolean availableForSpending = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private UserGroup group;

    @PrePersist
    public void prePersist() {
        if (this.balance == null){
            this.balance = BigDecimal.ZERO;
        }
    }
}
