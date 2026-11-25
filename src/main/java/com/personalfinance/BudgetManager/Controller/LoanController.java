package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.Controller.DTO.CustomUserDetails;
import com.personalfinance.BudgetManager.DTO.CreateLoanRequest;
import com.personalfinance.BudgetManager.DTO.LoanDTO;
import com.personalfinance.BudgetManager.DTO.UpdateLoanRequest;
import com.personalfinance.BudgetManager.Services.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Kredyty. Usunięcie i archiwizacja idą przez konto: /api/accounts/{accountId}. */
@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping
    public ResponseEntity<List<LoanDTO>> getLoans(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(loanService.getLoans(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanDTO> getLoan(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(loanService.getLoan(user.getId(), id));
    }

    @PostMapping
    public ResponseEntity<LoanDTO> create(@Valid @RequestBody CreateLoanRequest request,
                                          @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.create(user.getId(), request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<LoanDTO> update(@PathVariable Long id, @Valid @RequestBody UpdateLoanRequest request,
                                          @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(loanService.update(user.getId(), id, request));
    }
}