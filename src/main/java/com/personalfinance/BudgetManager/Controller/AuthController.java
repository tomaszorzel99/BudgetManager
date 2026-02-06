package com.personalfinance.BudgetManager.Controller;


import com.personalfinance.BudgetManager.Configuration.JwtTokenProvider;
import com.personalfinance.BudgetManager.Controller.DTO.AuthRequest;
import com.personalfinance.BudgetManager.Controller.DTO.AuthResponse;
import com.personalfinance.BudgetManager.DTO.RegisterRequest;
import com.personalfinance.BudgetManager.Services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, AuthService authService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request){
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
            var authToken = new UsernamePasswordAuthenticationToken(request.email(), request.password());
            var auth = authenticationManager.authenticate(authToken);
            var token = jwtTokenProvider.createToken(auth.getName());
            return ResponseEntity.ok(new AuthResponse(token));
    }
}
