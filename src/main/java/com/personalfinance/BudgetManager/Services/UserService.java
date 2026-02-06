package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.UpdateUserRequest;
import com.personalfinance.BudgetManager.Exception.EmailException;
import com.personalfinance.BudgetManager.Exception.UserException;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new UserException(email));
    }

    @Transactional()
    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return getUserByEmail(email);
    }

//    public User getUserById(Long id) {
//        return userRepository.findById(id).orElseThrow(() -> new UserException(id));
//    }

    @Transactional
    public User updateUser(String currentEmail, UpdateUserRequest request) {
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new UserException(currentEmail));
        updateExistingUserWithPatch(user, request);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException(email));
        user.setEmail("Deleted" + UUID.randomUUID() + "_" + email);
        user.setName("DELETED");
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setEnabled(false);
        userRepository.save(user);
    }

    public void updateExistingUserWithPatch(User existingUser, UpdateUserRequest request) {
        if (request.getName() != null) existingUser.setName(request.getName());
        if (request.getEmail() != null && !request.getEmail().equals(existingUser.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new EmailException(request.getEmail());
            }
            existingUser.setEmail(request.getEmail());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            String encodedPassword = passwordEncoder.encode(request.getPassword());
            existingUser.setPassword(encodedPassword);
        }
    }
}

