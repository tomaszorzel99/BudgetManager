package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.RegisterRequest;
import com.personalfinance.BudgetManager.Exception.EmailException;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Model.UserGroup;
import com.personalfinance.BudgetManager.Repositories.UserGroupRepository;
import com.personalfinance.BudgetManager.Repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, UserGroupRepository userGroupRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userGroupRepository = userGroupRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegisterRequest request){
        log.info("Attempting to register new user with email: {}", request.getEmail());

        if(userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Email {} is already taken", request.getEmail());
            throw new EmailException(request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);

//        UserGroup group = new UserGroup();
//        group.setName("Group of: " + user.getEmail());
//
//        user.getUserGroups().add(group);
//        group.getUsers().add(user);

        userRepository.save(user);
        log.info("User {} successfully registered", request.getEmail());
    }

}
