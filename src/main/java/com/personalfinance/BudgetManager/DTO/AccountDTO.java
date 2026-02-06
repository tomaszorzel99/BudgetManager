package com.personalfinance.BudgetManager.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AccountDTO {

    private Long id;
    private String name;
    private String accountNumber;
    private String currency;
    private BigDecimal balance;
    private String userName;
    private String groupName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
