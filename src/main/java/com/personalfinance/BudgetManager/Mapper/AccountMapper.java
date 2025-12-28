package com.personalfinance.BudgetManager.Mapper;

import com.personalfinance.BudgetManager.DTO.AccountDTO;
import com.personalfinance.BudgetManager.Model.Account;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccountMapper {
    public AccountDTO convertToDTO(Account account){
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setId(account.getId());
        accountDTO.setName(account.getName());
        accountDTO.setAccountNumber(account.getAccountNumber());
        accountDTO.setCurrency(account.getCurrency());
        accountDTO.setBalance(account.getBalance());
        accountDTO.setCreateDate(account.getCreateDate());
        accountDTO.setUserName(account.getUser().getName());
        return accountDTO;
    }

    public List<AccountDTO> convertToListDTO (List<Account> accounts){
        return accounts.stream()
                .map(this::convertToDTO)
                .toList();
    }
}
