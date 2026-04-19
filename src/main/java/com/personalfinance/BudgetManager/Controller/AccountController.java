package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.Controller.DTO.CustomUserDetails;
import com.personalfinance.BudgetManager.DTO.AccountDTO;
import com.personalfinance.BudgetManager.DTO.CreateAccountRequest;
import com.personalfinance.BudgetManager.DTO.UpdateAccountRequest;
import com.personalfinance.BudgetManager.Mapper.AccountMapper;
import com.personalfinance.BudgetManager.Model.Account;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Services.AccountService;
import com.personalfinance.BudgetManager.Services.UserService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;
    private final UserService userService;

    public AccountController(AccountService accountService, AccountMapper accountMapper, UserService userService) {
        this.accountService = accountService;
        this.accountMapper = accountMapper;
        this.userService = userService;
    }


    @PostMapping
    @Transactional
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

    @PatchMapping("/{id}/archive")
    public ResponseEntity<Void> archiveAccount(@PathVariable Long id,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        accountService.archiveAccount(id,  userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AccountDTO> deleteAccount(@PathVariable Long id,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        accountService.deleteAccountById(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
