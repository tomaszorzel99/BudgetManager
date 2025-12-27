package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.DTO.CreateUserRequest;
import com.personalfinance.BudgetManager.DTO.UserDTO;
import com.personalfinance.BudgetManager.Mapper.UserMapper;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request){
        User user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.convertToDTO(user));
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getUsers(){
        List<User> allUsers = userService.getAllUsers();
        return ResponseEntity.ok(userMapper.convertToListDTO(allUsers));
    }
}
