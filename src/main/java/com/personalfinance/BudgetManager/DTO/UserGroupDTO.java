package com.personalfinance.BudgetManager.DTO;

import lombok.Data;

import java.util.List;

@Data
public class UserGroupDTO {

    private Long id;
    private String name;
    private Long ownerId;
    private List<GroupMemberDTO> members;
}