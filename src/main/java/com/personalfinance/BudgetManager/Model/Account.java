package com.personalfinance.BudgetManager.Model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Account name cannot be blank")
    private String name;

    @Column(unique = true)
    private String accountNumber;

    @NotBlank(message = "Currence cannot be blank")
    private String currency;

    @Min(value = 0, message = "Balance cannot be negative")
    private BigDecimal balance;

    @Column(name = "created_date")
    private LocalDateTime createDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    public void prePersist() {
        this.createDate = LocalDateTime.now();
        if (this.balance == null){
            this.balance = BigDecimal.ZERO;
        }
    }
}
