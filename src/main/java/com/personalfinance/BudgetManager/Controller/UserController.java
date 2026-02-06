package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.DTO.CreateUserRequest;
import com.personalfinance.BudgetManager.DTO.UpdateUserRequest;
import com.personalfinance.BudgetManager.DTO.UserDTO;
import com.personalfinance.BudgetManager.Mapper.UserMapper;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;


    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(){
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(userMapper.convertToDTO(currentUser));
    }

//    @GetMapping("/{id}")
//    public ResponseEntity<UserDTO> getUserByID(@PathVariable Long id){
//        User userById = userService.getUserById(id);
//        return ResponseEntity.ok(userMapper.convertToDTO(userById));
//    }


    @PatchMapping("/me")
    public ResponseEntity<UserDTO> updateUser(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody UpdateUserRequest request){
        User updatedUser = userService.updateUser(userDetails.getUsername(), request);
        return ResponseEntity.ok().body(userMapper.convertToDTO(updatedUser));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal UserDetails userDetails){
        userService.deleteUser(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
