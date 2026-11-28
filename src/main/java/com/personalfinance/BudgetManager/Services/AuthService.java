package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.Configuration.JwtTokenProvider;
import com.personalfinance.BudgetManager.Controller.DTO.CustomUserDetails;
import com.personalfinance.BudgetManager.DTO.RegisterRequest;
import com.personalfinance.BudgetManager.Exception.EmailException;
import com.personalfinance.BudgetManager.Exception.TooManyLoginAttemptsException;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Repositories.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service
public class AuthService {

    private static final int MAX_FAILURES_PER_EMAIL = 5;
    private static final int MAX_FAILURES_PER_IP = 20;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginAttemptService loginAttemptService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.loginAttemptService = loginAttemptService;
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public void register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            log.warn("Registration failed: email already taken");
            throw new EmailException(email);
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);

        userRepository.save(user);
        log.info("User registered, id={}", user.getId());
    }

    public String login(String rawEmail, String password, String clientIp) {
        String email = normalizeEmail(rawEmail);
        String emailKey = "email:" + email;
        String ipKey = "ip:" + clientIp;

        if (loginAttemptService.isBlocked(emailKey, MAX_FAILURES_PER_EMAIL)
                || loginAttemptService.isBlocked(ipKey, MAX_FAILURES_PER_IP)) {
            throw new TooManyLoginAttemptsException();
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));
            loginAttemptService.loginSucceeded(emailKey);
            CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
            return jwtTokenProvider.createToken(user.getUsername(), user.getTokenVersion());
        } catch (AuthenticationException e) {
            loginAttemptService.loginFailed(emailKey);
            loginAttemptService.loginFailed(ipKey);
            throw e;
        }
    }

    @Transactional
    public void logout(Long userId) {
        userRepository.findById(userId).ifPresent(User::revokeTokens);
    }
}