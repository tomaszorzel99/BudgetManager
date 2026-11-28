package com.personalfinance.BudgetManager.Mapper;

import com.personalfinance.BudgetManager.DTO.TransactionDTO;
import com.personalfinance.BudgetManager.Model.Transaction;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransactionMapper {
    public TransactionDTO convertToDTO(Transaction transaction){
        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setId(transaction.getId());
        transactionDTO.setAmount(transaction.getAmount());
        transactionDTO.setType(transaction.getType());
        transactionDTO.setDescription(transaction.getDescription());
        transactionDTO.setTransactionDate(transaction.getTransactionDate());
        transactionDTO.setCreatedDate(transaction.getCreatedAt());
        transactionDTO.setUserName(transaction.getUser().getName());
        transactionDTO.setCategoryId(transaction.getCategory().getId());
        transactionDTO.setCategoryName(transaction.getCategory().getName());
        if (transaction.getSubcategory() != null) {
            transactionDTO.setSubcategoryId(transaction.getSubcategory().getId());
            transactionDTO.setSubcategoryName(transaction.getSubcategory().getName());
        }
        transactionDTO.setAccountId(transaction.getAccount().getId());
        transactionDTO.setAccountName(transaction.getAccount().getName());
        transactionDTO.setCurrency(transaction.getAccount().getCurrency());
        return transactionDTO;
    }

    public List<TransactionDTO> convertToListDTO(List<Transaction> transactions){
        return transactions.stream()
                .map(this::convertToDTO)
                .toList();
    }
}