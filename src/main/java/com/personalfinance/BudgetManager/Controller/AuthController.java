package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.Controller.DTO.AuthRequest;
import com.personalfinance.BudgetManager.Controller.DTO.AuthResponse;
import com.personalfinance.BudgetManager.Controller.DTO.CustomUserDetails;
import com.personalfinance.BudgetManager.DTO.RegisterRequest;
import com.personalfinance.BudgetManager.Services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request,
                                              HttpServletRequest httpRequest) {
        String token = authService.login(request.email(), request.password(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserDetails user) {
        if (user != null) {
            authService.logout(user.getId());
        }
        return ResponseEntity.noContent().build();
    }
}