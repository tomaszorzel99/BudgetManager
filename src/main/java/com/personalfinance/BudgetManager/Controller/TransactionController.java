package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.DTO.CreateTransactionRequest;
import com.personalfinance.BudgetManager.DTO.TransactionDTO;
import com.personalfinance.BudgetManager.Mapper.TransactionMapper;
import com.personalfinance.BudgetManager.Model.CategoryType;
import com.personalfinance.BudgetManager.Model.Transaction;
import com.personalfinance.BudgetManager.Services.GroupService;
import com.personalfinance.BudgetManager.Services.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    public TransactionController(TransactionService transactionService, TransactionMapper transactionMapper) {
        this.transactionService = transactionService;
        this.transactionMapper = transactionMapper;
    }

    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(@Valid @RequestBody CreateTransactionRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        Transaction transaction = transactionService.createTransaction(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionMapper.convertToDTO(transaction));
    }

    @GetMapping()
    public ResponseEntity<List<TransactionDTO>> getTransactions(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) CategoryType categoryType,
            @RequestParam(required = false) Long categoryId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails.getUsername();
        List<Transaction> transactions = transactionService.getTransactions(groupId, userEmail, month, year, categoryType, categoryId);
        return ResponseEntity.ok(transactionMapper.convertToListDTO(transactions));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        String userEmail = userDetails.getUsername();
        transactionService.deleteTransactionById(id, userEmail);
        return ResponseEntity.noContent().build();
    }
}
