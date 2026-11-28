package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.DTO.*;
import com.personalfinance.BudgetManager.Mapper.TransactionMapper;
import com.personalfinance.BudgetManager.Model.CategoryType;
import com.personalfinance.BudgetManager.Model.Transaction;
import com.personalfinance.BudgetManager.Services.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
            @RequestParam(defaultValue = "false") boolean mine,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails.getUsername();
        List<Transaction> transactions = transactionService.getTransactions(groupId, userEmail, month, year, categoryType, categoryId, mine);
        return ResponseEntity.ok(transactionMapper.convertToListDTO(transactions));
    }

    @GetMapping("/page")
    public ResponseEntity<TransactionPageDTO> getTransactionPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Long groupId,
            @RequestParam(defaultValue = "false") boolean mine,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) CategoryType type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String q,
            @AuthenticationPrincipal UserDetails userDetails) {

        TransactionFilter filter = new TransactionFilter(groupId, mine, from, to, type, categoryId, accountId, q);
        TransactionService.TransactionPage result =
                transactionService.getTransactionPage(userDetails.getUsername(), filter, page, size);
        Page<Transaction> p = result.page();
        return ResponseEntity.ok(new TransactionPageDTO(transactionMapper.convertToListDTO(p.getContent()),
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(), result.totals()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransactionById(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TransactionDTO> updateTransaction(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateTransactionRequest request) {
        Transaction transaction = transactionService.updateTransaction(id, request);
        return ResponseEntity.ok(transactionMapper.convertToDTO(transaction));
    }
}
