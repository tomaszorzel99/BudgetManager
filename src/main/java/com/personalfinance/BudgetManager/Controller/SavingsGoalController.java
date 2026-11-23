package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.Controller.DTO.CustomUserDetails;
import com.personalfinance.BudgetManager.DTO.CreateSavingsGoalRequest;
import com.personalfinance.BudgetManager.DTO.GoalContributionRequest;
import com.personalfinance.BudgetManager.DTO.SavingsGoalDTO;
import com.personalfinance.BudgetManager.DTO.UpdateSavingsGoalRequest;
import com.personalfinance.BudgetManager.Services.SavingsGoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class SavingsGoalController {

    private final SavingsGoalService goalService;

    public SavingsGoalController(SavingsGoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    public ResponseEntity<List<SavingsGoalDTO>> getGoals(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(goalService.getGoals(user.getId()));
    }

    @PostMapping
    public ResponseEntity<SavingsGoalDTO> create(@Valid @RequestBody CreateSavingsGoalRequest request,
                                                 @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(goalService.create(user.getId(), request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SavingsGoalDTO> update(@PathVariable Long id, @Valid @RequestBody UpdateSavingsGoalRequest request,
                                                 @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(goalService.update(user.getId(), id, request));
    }

    @PostMapping("/{id}/contribute")
    public ResponseEntity<SavingsGoalDTO> contribute(@PathVariable Long id, @Valid @RequestBody GoalContributionRequest request,
                                                     @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(goalService.contribute(user.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        goalService.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}

