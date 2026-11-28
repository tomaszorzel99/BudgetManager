package com.personalfinance.BudgetManager.Mapper;

import com.personalfinance.BudgetManager.DTO.TransferDTO;
import com.personalfinance.BudgetManager.Model.Transfer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransferMapper {

    public TransferDTO convertToDTO(Transfer transfer) {
        TransferDTO transferDTO = new TransferDTO();
        transferDTO.setId(transfer.getId());
        transferDTO.setAmount(transfer.getAmount());
        transferDTO.setReceivedAmount(transfer.getReceivedAmount());
        transferDTO.setExchangeRate(transfer.getExchangeRate());
        transferDTO.setFromCurrency(transfer.getFromAccount().getCurrency());
        transferDTO.setToCurrency(transfer.getToAccount().getCurrency());
        transferDTO.setDescription(transfer.getDescription());
        transferDTO.setTransferDate(transfer.getTransferDate());

        transferDTO.setFromAccountId(transfer.getFromAccount().getId());
        transferDTO.setFromAccountName(transfer.getFromAccount().getName());
        transferDTO.setFromAccountType(transfer.getFromAccount().getAccountType());

        transferDTO.setToAccountId(transfer.getToAccount().getId());
        transferDTO.setToAccountName(transfer.getToAccount().getName());
        transferDTO.setToAccountType(transfer.getToAccount().getAccountType());

        transferDTO.setUserId(transfer.getUser().getId());
        transferDTO.setUserName(transfer.getUser().getName());

        transferDTO.setCreatedAt(transfer.getCreatedAt());
        transferDTO.setUpdatedAt(transfer.getUpdatedAt());

        return transferDTO;
    }

    public List<TransferDTO> convertToDTOList(List<Transfer> transfers) {
        return transfers.stream()
                .map(this::convertToDTO)
                .toList();
    }

}
