package com.personalfinance.BudgetManager.Mapper;

import com.personalfinance.BudgetManager.DTO.TransactionDTO;
import com.personalfinance.BudgetManager.DTO.UserDTO;
import com.personalfinance.BudgetManager.Model.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {
    public UserDTO convertToDTO(User user){
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setName(user.getName());
        userDTO.setEmail(user.getEmail());
        userDTO.setCreatedDate(user.getCreateData());
        return userDTO;
    }

    public List<UserDTO> convertToListDTO(List<User> users){
        return users.stream()
                .map(this::convertToDTO)
                .toList();
    }
}
