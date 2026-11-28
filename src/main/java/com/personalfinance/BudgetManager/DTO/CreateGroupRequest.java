package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateGroupRequest {

    @NotBlank(message = "Group name cannot be blank")
    @Size(max = 255, message = "Group name is too long")
    private String groupName;
}