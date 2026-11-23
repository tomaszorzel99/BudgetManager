package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InvitationTokenRequest {

    @NotBlank(message = "Invitation token cannot be blank")
    @Size(max = 64, message = "Invitation token is invalid or does not exist")
    private String token;
}