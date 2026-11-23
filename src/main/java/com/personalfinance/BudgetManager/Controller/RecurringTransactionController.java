package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.Controller.DTO.CustomUserDetails;
import com.personalfinance.BudgetManager.DTO.*;
import com.personalfinance.BudgetManager.Services.RecurringTransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/recurring")
public class RecurringTransactionController {

    private final RecurringTransactionService recurringService;

    public RecurringTransactionController(RecurringTransactionService recurringService) {
        this.recurringService = recurringService;
    }

    @GetMapping
    public ResponseEntity<List<RecurringTransactionDTO>> getAll(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(recurringService.getAll(user.getId()));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<UpcomingPaymentDTO>> upcoming(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(recurringService.upcoming(user.getId(), from, to));
    }

    @PostMapping
    public ResponseEntity<RecurringTransactionDTO> create(@Valid @RequestBody CreateRecurringTransactionRequest request,
                                                          @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recurringService.create(user.getId(), request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RecurringTransactionDTO> update(@PathVariable Long id,
                                                          @Valid @RequestBody UpdateRecurringTransactionRequest request,
                                                          @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(recurringService.update(user.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        recurringService.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/post")
    public ResponseEntity<RecurringTransactionDTO> post(@PathVariable Long id,
                                                        @Valid @RequestBody(required = false) PostRecurringRequest request,
                                                        @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(recurringService.post(user.getId(), id, request));
    }

    @PostMapping("/{id}/skip")
    public ResponseEntity<RecurringTransactionDTO> skip(@PathVariable Long id,
                                                        @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(recurringService.skip(user.getId(), id));
    }
}
