package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.DTO.AccountDTO;
import com.personalfinance.BudgetManager.DTO.CreateAccountRequest;
import com.personalfinance.BudgetManager.Mapper.AccountMapper;
import com.personalfinance.BudgetManager.Model.Account;
import com.personalfinance.BudgetManager.Services.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;

    public AccountController(AccountService accountService, AccountMapper accountMapper) {
        this.accountService = accountService;
        this.accountMapper = accountMapper;
    }

    @PostMapping
    public ResponseEntity<AccountDTO> createAccount(@Valid @RequestBody CreateAccountRequest request){
        Account account = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountMapper.convertToDTO(account));
    }

    @GetMapping
    public ResponseEntity<List<AccountDTO>> getAllAcounts(){
        List<Account> allAccounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accountMapper.convertToListDTO(allAccounts));
    }


}
