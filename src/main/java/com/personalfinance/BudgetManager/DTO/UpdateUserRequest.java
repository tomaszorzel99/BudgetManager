package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUserRequest {

    private String name;

    @Email(message = "Email should be valid")
    private String email;

    private String password;
}

