package com.personalfinance.BudgetManager.Mapper;

import com.personalfinance.BudgetManager.DTO.UserDTO;
import com.personalfinance.BudgetManager.DTO.UserGroupDTO;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Model.UserGroup;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {
    public UserDTO convertToDTO(User user){
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setName(user.getName());
        userDTO.setEmail(user.getEmail());
        userDTO.setGroups(user.getUserGroups().stream().map(this::mapGroup).toList());
        userDTO.setCreatedAt(user.getCreatedAt());
        userDTO.setUpdatedAt(user.getUpdatedAt());
        return userDTO;
    }

    private UserGroupDTO mapGroup(UserGroup group) {
        UserGroupDTO userGroupDTO = new UserGroupDTO();
        userGroupDTO.setId(group.getId());
        userGroupDTO.setName(group.getName());
        return userGroupDTO;
    }

//    public List<UserDTO> convertToListDTO(List<User> users){
//        return users.stream()
//                .map(this::convertToDTO)
//                .toList();
//    } DO USUNIĘCIA
}
