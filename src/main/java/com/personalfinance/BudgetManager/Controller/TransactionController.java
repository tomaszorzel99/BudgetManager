package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.DTO.CreateTransactionRequest;
import com.personalfinance.BudgetManager.DTO.TransactionDTO;
import com.personalfinance.BudgetManager.Model.Transaction;
import com.personalfinance.BudgetManager.Services.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(@Valid @RequestBody CreateTransactionRequest request){
        Transaction transaction = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(transaction));
    }

    private TransactionDTO convertToDTO(Transaction transaction){
        TransactionDTO dto = new TransactionDTO();
        dto.setId(transaction.getId());
        dto.setAmount(transaction.getAmount());
        dto.setType(transaction.getType());
        dto.setDescription(transaction.getDescription());
        dto.setNotes(transaction.getNotes());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setUserName(transaction.getUser().getName());
        dto.setCategoryName(transaction.getCategory().getName());
        dto.setSubcategoryName(transaction.getSubcategory().getName());
        return dto;
    }

    private List<TransactionDTO> convertToLisDTO(List<Transaction> transactions){
        return transactions.stream()
                .map(this::convertToDTO)
                .toList();
    }
}
