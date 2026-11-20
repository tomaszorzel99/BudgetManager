package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(min = 1, max = 100, message = "Name must be 1-100 characters")
    private String name;

    @Email(message = "Email should be valid")
    @Size(max = 255)
    private String email;

    @Size(min = 8, max = 64, message = "Password must be 8-64 characters")
    private String password;

    /** Wymagane, gdy zmienia się e-mail albo hasło. */
    private String currentPassword;
}