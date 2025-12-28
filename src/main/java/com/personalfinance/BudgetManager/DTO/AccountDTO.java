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
    private LocalDateTime createDate;
    private String userName;
}
