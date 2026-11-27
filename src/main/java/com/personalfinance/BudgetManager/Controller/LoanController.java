package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.Controller.DTO.CustomUserDetails;
import com.personalfinance.BudgetManager.DTO.*;
import com.personalfinance.BudgetManager.Model.LoanPaymentKind;
import com.personalfinance.BudgetManager.Services.LoanPaymentService;
import com.personalfinance.BudgetManager.Services.LoanService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Kredyty. Usunięcie i archiwizacja idą przez konto: /api/accounts/{accountId}. */
@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;
    private final LoanPaymentService paymentService;

    public LoanController(LoanService loanService, LoanPaymentService paymentService) {
        this.loanService = loanService;
        this.paymentService = paymentService;
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

    // ---------------------------------------------------------------- spłaty

    @GetMapping("/{id}/payments")
    public ResponseEntity<List<LoanPaymentDTO>> getPayments(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(paymentService.list(user.getId(), id, from, to));
    }

    /** Podpowiedź kwot; bez daty: najbliższa rata albo nadpłata na dziś. */
    @GetMapping("/{id}/payments/suggest")
    public ResponseEntity<LoanPaymentSuggestionDTO> suggest(
            @PathVariable Long id,
            @RequestParam(required = false) LoanPaymentKind kind,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(paymentService.suggest(user.getId(), id, kind, date));
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<LoanPaymentDTO> recordPayment(@PathVariable Long id,
                                                        @Valid @RequestBody LoanPaymentRequest request,
                                                        @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.record(user.getId(), id, request));
    }

    @PutMapping("/{id}/payments/{paymentId}")
    public ResponseEntity<LoanPaymentDTO> updatePayment(@PathVariable Long id, @PathVariable Long paymentId,
                                                        @Valid @RequestBody LoanPaymentRequest request,
                                                        @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(paymentService.update(user.getId(), id, paymentId, request));
    }

    @DeleteMapping("/{id}/payments/{paymentId}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id, @PathVariable Long paymentId,
                                              @AuthenticationPrincipal CustomUserDetails user) {
        paymentService.delete(user.getId(), id, paymentId);
        return ResponseEntity.noContent().build();
    }

    /** Wyrównanie długu do kwoty „pozostało do spłaty” z wyciągu. */
    @PostMapping("/{id}/reconcile")
    public ResponseEntity<LoanDTO> reconcile(@PathVariable Long id, @Valid @RequestBody ReconcileLoanRequest request,
                                             @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(paymentService.reconcile(user.getId(), id, request));
    }
}