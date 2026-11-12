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

import java.math.BigDecimal;
import java.math.RoundingMode;
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

//        accountBalanceService.transfer(
//                request.getFromAccountId(),
//                request.getToAccountId(),
//                request.getAmount()
//
//        );

        BigDecimal rate = resolveRate(request, fromAccount, toAccount);
        BigDecimal receivedAmount = receivedAmount(request.getAmount(), rate);
        accountBalanceService.transfer(fromAccount.getId(), toAccount.getId(),
                request.getAmount(), receivedAmount);
        Transfer transfer = new Transfer();
        transfer.setFromAccount(fromAccount);
        transfer.setToAccount(toAccount);
        transfer.setAmount(request.getAmount());
        transfer.setReceivedAmount(receivedAmount);
        transfer.setExchangeRate(rate);
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

    @Transactional
    public Transfer updateTransfer(Long transferId, CreateTransferRequest request, String userEmail) {
        validateTransfer(request);
        Transfer transfer = getTransferForWrite(transferId, userEmail);
        User user = transfer.getUser();
        Account fromAccount = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new AccountException(request.getFromAccountId()));
        Account toAccount = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new AccountException(request.getToAccountId()));
        validateUserOwnsAccount(user, fromAccount, toAccount);
        BigDecimal rate = resolveRate(request, fromAccount, toAccount);
        BigDecimal received = receivedAmount(request.getAmount(), rate);

        accountBalanceService.replaceTransfer(transfer, fromAccount.getId(), toAccount.getId(),
                request.getAmount(), received);
        transfer.setFromAccount(fromAccount);
        transfer.setToAccount(toAccount);
        transfer.setAmount(request.getAmount());
        transfer.setReceivedAmount(received);
        transfer.setExchangeRate(rate);
        transfer.setDescription(request.getDescription());
        transfer.setTransferDate(request.getTransferDate() != null ? request.getTransferDate() : transfer.getTransferDate());
        return transferRepository.save(transfer);
    }


    @Transactional
    public void deleteTransfer(Long transferId, String userEmail) {

//        Transfer transfer = getTransferById(transferId, userEmail);
        Transfer transfer = getTransferForWrite(transferId, userEmail);
        accountBalanceService.transfer(
                transfer.getToAccount().getId(),
                transfer.getFromAccount().getId(),
                transfer.getReceivedAmount(),
                transfer.getAmount());
        transferRepository.delete(transfer);
    }

    private void validateTransfer(CreateTransferRequest request) {
        if (request.getFromAccountId() == null || request.getToAccountId() == null) {
            throw new IllegalArgumentException("Both accounts are required");
        }
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

    private BigDecimal receivedAmount(BigDecimal sourceAmount, BigDecimal rate) {
        BigDecimal result = sourceAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        if (result.signum() <= 0) {
            throw new IllegalArgumentException("Converted amount is too small after rounding");
        }
        return result;
    }

    private BigDecimal resolveRate(CreateTransferRequest request, Account from, Account to) {
        BigDecimal amount = request.getAmount();
        if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Source amount must be positive and have at most two decimal places");
        }
        if (from.getCurrency().equalsIgnoreCase(to.getCurrency())) {
            if (request.getExchangeRate() != null && request.getExchangeRate().compareTo(BigDecimal.ONE) != 0) {
                throw new IllegalArgumentException("Exchange rate must be 1 for accounts in the same currency");
            }
            return BigDecimal.ONE;
        }
        BigDecimal rate = request.getExchangeRate();
        if (rate == null || rate.signum() <= 0 || rate.scale() > 12) {
            throw new IllegalArgumentException("A positive exchange rate is required for different currencies");
        }
        return rate;
    }

    private Transfer getTransferForWrite(Long transferId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserException(userEmail));
        Transfer transfer = transferRepository.findByIdWithLock(transferId)
                .orElseThrow(() -> new TransferException(transferId));
        if (!transfer.getUser().getId().equals(user.getId())) {
            throw new SecurityException("You don't have access to this transfer");
        }
        return transfer;
    }
}