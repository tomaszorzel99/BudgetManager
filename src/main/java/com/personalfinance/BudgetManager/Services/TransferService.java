package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.CreateTransferRequest;
import com.personalfinance.BudgetManager.Exception.AccountException;
import com.personalfinance.BudgetManager.Exception.TransferException;
import com.personalfinance.BudgetManager.Exception.UserException;
import com.personalfinance.BudgetManager.Model.Account;
import com.personalfinance.BudgetManager.Model.Transfer;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Repositories.AccountRepository;
import com.personalfinance.BudgetManager.Repositories.TransferRepository;
import com.personalfinance.BudgetManager.Repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TransferService {


    private final TransferRepository transferRepository;
    private final AccountBalanceService accountBalanceService;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public TransferService(TransferRepository transferRepository, AccountBalanceService accountBalanceService, AccountRepository accountRepository, UserRepository userRepository) {
        this.transferRepository = transferRepository;
        this.accountBalanceService = accountBalanceService;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Transfer createTransfer(CreateTransferRequest request, String userEmail) {
        validateTransfer(request);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserException(userEmail));

        Account fromAccount = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new AccountException(request.getFromAccountId()));

        Account toAccount = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new AccountException(request.getToAccountId()));

        validateUserOwnsAccount(user, fromAccount, toAccount);

        accountBalanceService.transfer(
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount()

        );
        Transfer transfer = new Transfer();
        transfer.setFromAccount(fromAccount);
        transfer.setToAccount(toAccount);
        transfer.setAmount(request.getAmount());
        transfer.setDescription(request.getDescription());
        transfer.setTransferDate(request.getTransferDate() != null ? request.getTransferDate() : LocalDate.now());
        transfer.setUser(user);

        return transferRepository.save(transfer);
    }

    public List<Transfer> getTransfers(String userEmail, Long accountId, Integer month, Integer year) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserException(userEmail));

        if (accountId != null && month != null && year != null) {
            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = startDate.plusMonths(1).minusDays(1);
            return transferRepository.findByUserAndAccountAndDateBetween(user.getId(), accountId, startDate, endDate);
        } else if (accountId != null) {
            return transferRepository.findByUserAndAccount(user.getId(), accountId);
        } else if (month != null && year != null) {
            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = startDate.plusMonths(1).minusDays(1);
            return transferRepository.findByUserAndDateBetween(user.getId(), startDate, endDate);
        } else {
            return transferRepository.findByUserId(user.getId());
        }
    }

    public Transfer getTransferById(Long transferId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserException(userEmail));

        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new TransferException(transferId));

        if (!transfer.getUser().getId().equals(user.getId())) {
            throw new SecurityException("You don't have access to this transfer");
        }
        return transfer;
    }

    public void deleteTransfer(Long transferId, String userEmail) {

        Transfer transfer = getTransferById(transferId, userEmail);

        accountBalanceService.transfer(
                transfer.getToAccount().getId(),
                transfer.getFromAccount().getId(),
                transfer.getAmount());
        transferRepository.delete(transfer);
    }

    private void validateTransfer(CreateTransferRequest request) {
        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
    }

    private void validateUserOwnsAccount(User user, Account fromAccount, Account toAccount) {
        boolean hasAccessToFromAccount = fromAccount.getGroup().getUsers().contains(user);
        boolean hasAccessToToAccount = toAccount.getGroup().getUsers().contains(user);

        if (!hasAccessToFromAccount || !hasAccessToToAccount) {
            throw new SecurityException(
                    "User does not belong to the group(s) that own these accounts");
        }
    }
}