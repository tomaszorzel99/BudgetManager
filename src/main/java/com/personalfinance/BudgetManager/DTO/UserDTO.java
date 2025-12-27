package com.personalfinance.BudgetManager.DTO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDTO {

    private Long id;
    private String name;
    private String email;
    private LocalDateTime createdDate;
}
