package com.personalfinance.BudgetManager.Mapper;

import com.personalfinance.BudgetManager.DTO.GroupMemberDTO;
import com.personalfinance.BudgetManager.DTO.UserDTO;
import com.personalfinance.BudgetManager.DTO.UserGroupDTO;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Model.UserGroup;
import org.springframework.stereotype.Component;

import java.util.Comparator;


@Component
public class UserMapper {
    public UserDTO convertToDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setName(user.getName());
        userDTO.setEmail(user.getEmail());
        userDTO.setGroups(user.getUserGroups().stream()
                .sorted(Comparator.comparing(UserGroup::getId))
                .map(this::mapGroup)
                .toList());
        userDTO.setCreatedAt(user.getCreatedAt());
        userDTO.setUpdatedAt(user.getUpdatedAt());
        return userDTO;
    }

    public UserGroupDTO mapGroup(UserGroup group) {
        UserGroupDTO userGroupDTO = new UserGroupDTO();
        userGroupDTO.setId(group.getId());
        userGroupDTO.setName(group.getName());
        userGroupDTO.setOwnerId(group.getOwner() == null ? null : group.getOwner().getId());
        userGroupDTO.setMembers(group.getUsers().stream()
                .filter(User::isEnabled)
                .sorted(Comparator.comparing(User::getId))
                .map(member -> new GroupMemberDTO(member.getId(), member.getName()))
                .toList());
        return userGroupDTO;
    }
}
