package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.UserGroup;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserDTO {

    private Long id;
    private String name;
    private String email;
    private List<UserGroupDTO> groups;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
