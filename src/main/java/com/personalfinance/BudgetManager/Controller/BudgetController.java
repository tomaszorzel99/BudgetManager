package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.Controller.DTO.CustomUserDetails;
import com.personalfinance.BudgetManager.DTO.BudgetDTO;
import com.personalfinance.BudgetManager.DTO.CreateBudgetRequest;
import com.personalfinance.BudgetManager.DTO.UpdateBudgetRequest;
import com.personalfinance.BudgetManager.Services.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public ResponseEntity<List<BudgetDTO>> getBudgets(@RequestParam(required = false) Integer month,
                                                      @RequestParam(required = false) Integer year,
                                                      @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(budgetService.getBudgets(user.getId(), month, year));
    }

    @PostMapping
    public ResponseEntity<BudgetDTO> createBudget(@Valid @RequestBody CreateBudgetRequest request,
                                                  @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.createBudget(user.getId(), request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BudgetDTO> updateBudget(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateBudgetRequest request,
                                                  @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(budgetService.updateBudget(user.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id,
                                             @AuthenticationPrincipal CustomUserDetails user) {
        budgetService.deleteBudget(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
