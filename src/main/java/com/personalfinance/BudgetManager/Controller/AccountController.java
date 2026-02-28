package com.personalfinance.BudgetManager.Controller;

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
    public ResponseEntity<AccountDTO> createAccount(@Valid @RequestBody CreateAccountRequest request, @AuthenticationPrincipal UserDetails userDetails){
        Account account = accountService.createAccount(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(accountMapper.convertToDTO(account));
    }

    @GetMapping
    public ResponseEntity<List<AccountDTO>> getAccounts(@AuthenticationPrincipal UserDetails userDetails){
        User user = userService.getUserByEmail(userDetails.getUsername());
        List<Account> accounts = accountService.getAccountsVisibleForUser(user);
        return ResponseEntity.ok(accountMapper.convertToListDTO(accounts));
    }

//    @PatchMapping("/{id}")
//    public ResponseEntity<AccountDTO> updateAccount(@PathVariable Long id, @Valid @RequestBody UpdateAccountRequest request, @AuthenticationPrincipal UserDetails userDetails) throws AccessDeniedException {
//        Account updatedAccount = accountService.updateAccount(id, request, userDetails.getUsername());
//        return ResponseEntity.ok().body(accountMapper.convertToDTO(updatedAccount));
//    }
//
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccountById(id);
        return ResponseEntity.noContent().build();
    }
}
