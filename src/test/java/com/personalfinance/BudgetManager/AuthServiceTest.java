package com.personalfinance.BudgetManager;

import com.personalfinance.BudgetManager.DTO.RegisterRequest;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Model.UserGroup;
import com.personalfinance.BudgetManager.Repositories.UserGroupRepository;
import com.personalfinance.BudgetManager.Repositories.UserRepository;
import com.personalfinance.BudgetManager.Services.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_ShouldSaveUserAndCreateGroup_WhenEmailUnique(){
        RegisterRequest registerRequest = new RegisterRequest("Tomasz", "tomek32161@gmail.com", "Tomek123");

        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");

        authService.register(registerRequest);

        verify(userRepository, atLeastOnce()).save(any(User.class));
        verify(userGroupRepository).save(any(UserGroup.class));
        verify(passwordEncoder).encode("Tomek123");
    }

    @Test
    void register_ShouldThrowException_WhenEmailIsAlreadyTaken() {

        RegisterRequest request = new RegisterRequest("Tomasz", "tomek32161@gmail.com", "Tomek123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> {
            authService.register(request);
        });

        verify(userRepository, never()).save(any(User.class));
    }
}
