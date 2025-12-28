package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.DTO.CreateTransactionRequest;
import com.personalfinance.BudgetManager.DTO.TransactionDTO;
import com.personalfinance.BudgetManager.Mapper.TransactionMapper;
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
    private final TransactionMapper transactionMapper;

    public TransactionController(TransactionService transactionService, TransactionMapper transactionMapper) {
        this.transactionService = transactionService;
        this.transactionMapper = transactionMapper;
    }

    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(@Valid @RequestBody CreateTransactionRequest request){
        Transaction transaction = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionMapper.convertToDTO(transaction));
    }

    @GetMapping
    public ResponseEntity<List<TransactionDTO>> getTransactions(){
        List<Transaction> allTransactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(transactionMapper.convertToListDTO(allTransactions));
    }
}
