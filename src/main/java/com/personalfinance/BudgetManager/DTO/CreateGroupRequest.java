package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateGroupRequest {

    @NotBlank
    private String groupName;
}
