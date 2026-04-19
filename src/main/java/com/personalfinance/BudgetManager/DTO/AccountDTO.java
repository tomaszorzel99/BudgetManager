package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.AccountType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AccountDTO {

    private Long id;
    private String name;
    private String currency;
    private BigDecimal balance;
    private AccountType accountType;
    private Boolean availableForSpending;
    private List<String> users;
    private String groupName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
