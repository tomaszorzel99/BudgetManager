package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.AccountType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TransferDTO {

    private Long id;
    private BigDecimal amount;
    private String description;
    private LocalDate transferDate;

    private Long fromAccountId;
    private String fromAccountName;
    private AccountType fromAccountType;


    private Long toAccountId;
    private String toAccountName;
    private AccountType toAccountType;

    private String userName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
