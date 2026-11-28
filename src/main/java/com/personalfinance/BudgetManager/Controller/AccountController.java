package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.Controller.DTO.CustomUserDetails;
import com.personalfinance.BudgetManager.DTO.AccountDTO;
import com.personalfinance.BudgetManager.DTO.AccountHistoryDTO;
import com.personalfinance.BudgetManager.DTO.CreateAccountRequest;
import com.personalfinance.BudgetManager.DTO.UpdateAccountRequest;
import com.personalfinance.BudgetManager.DTO.PropertyValueRequest;
import com.personalfinance.BudgetManager.Mapper.AccountMapper;
import com.personalfinance.BudgetManager.Model.Account;
import com.personalfinance.BudgetManager.Services.AccountHistoryService;
import com.personalfinance.BudgetManager.Services.AccountService;
import com.personalfinance.BudgetManager.Services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;
    private final UserService userService;
    private final AccountHistoryService accountHistoryService;

    public AccountController(AccountService accountService, AccountMapper accountMapper, UserService userService, AccountHistoryService accountHistoryService) {
        this.accountService = accountService;
        this.accountMapper = accountMapper;
        this.userService = userService;
        this.accountHistoryService = accountHistoryService;
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<AccountHistoryDTO> getAccountHistory(@PathVariable Long id,
                                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(accountHistoryService.getHistory(id, userDetails.getId()));
    }

    @PostMapping
    public ResponseEntity<AccountDTO> createAccount(@Valid @RequestBody CreateAccountRequest request,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails){
        Account account = accountService.createAccount(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(accountMapper.convertToDTO(account));
    }

    @GetMapping
    public ResponseEntity<List<AccountDTO>> getAccounts(@AuthenticationPrincipal CustomUserDetails userDetails){
        return ResponseEntity.ok(accountMapper.convertToListDTO(accountService.getAccountsVisibleForUser(userDetails.getId())));
    }

    @GetMapping("/archived")
    public ResponseEntity<List<AccountDTO>> getArchivedAccounts(){
        return ResponseEntity.ok(accountMapper.convertToListDTO(accountService.getArchivedAccount()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AccountDTO> updateAccount(@PathVariable Long id, @Valid @RequestBody UpdateAccountRequest request) {
        Account updatedAccount = accountService.updateAccount(id, request);
        return ResponseEntity.ok().body(accountMapper.convertToDTO(updatedAccount));
    }

    @PostMapping("/{id}/value")
    public ResponseEntity<AccountDTO> revalueProperty(@PathVariable Long id, @Valid @RequestBody PropertyValueRequest request,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(accountMapper.convertToDTO(accountService.revalueProperty(id, userDetails.getId(), request)));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<Void> archiveAccount(@PathVariable Long id,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        accountService.archiveAccount(id,  userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/unarchive")
    public ResponseEntity<AccountDTO> unarchiveAccount(@PathVariable Long id,
                                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        Account account = accountService.unarchiveAccount(id, userDetails.getId());
        return ResponseEntity.ok(accountMapper.convertToDTO(account));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AccountDTO> deleteAccount(@PathVariable Long id,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        accountService.deleteAccountById(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
