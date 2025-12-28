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
        transactionDTO.setNotes(transaction.getNotes());
        transactionDTO.setCreatedDate(transaction.getCreatedDate());
        transactionDTO.setUserName(transaction.getUser().getName());
        transactionDTO.setCategoryName(transaction.getCategory().getName());
        transactionDTO.setSubcategoryName(transaction.getSubcategory().getName());
        transactionDTO.setAccountId(transaction.getAccount().getId());
        return transactionDTO;
    }

    public List<TransactionDTO> convertToListDTO(List<Transaction> transactions){
        return transactions.stream()
                .map(this::convertToDTO)
                .toList();
    }
}
