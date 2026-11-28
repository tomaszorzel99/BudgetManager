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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        rejectLoanAccounts(fromAccount, toAccount);

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

    /**
     * Przelewy, w których jedno z kont należy do grup użytkownika (bez groupId: wszystkich jego grup),
     * od najnowszych. mine = true zostawia tylko przelewy zalogowanej osoby.
     */
    @Transactional(readOnly = true)
    public List<Transfer> getTransfers(String userEmail, Long accountId, Integer month, Integer year,
                                       Long groupId, boolean mine) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserException(userEmail));
        if (groupId != null && user.getUserGroups().stream().noneMatch(g -> g.getId().equals(groupId))) {
            throw new AccessDeniedException("No access to this group");
        }
        List<Long> groupIds = groupId != null ? List.of(groupId) : CategoryService.groupIds(user);
        LocalDate from = null;
        LocalDate to = null;
        if (month != null && year != null) {
            from = LocalDate.of(year, month, 1);
            to = from.plusMonths(1).minusDays(1);
        }
        return transferRepository.findVisible(groupIds, mine ? user.getId() : null, accountId, from, to);
    }

    @Transactional(readOnly = true)
    public Transfer getTransferById(Long transferId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserException(userEmail));

        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new TransferException(transferId));

        if (!isMember(transfer, user.getId())) {
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
        rejectLoanAccounts(transfer.getFromAccount(), transfer.getToAccount());
        rejectLoanAccounts(fromAccount, toAccount);
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
        Transfer transfer = getTransferForWrite(transferId, userEmail);
        rejectLoanAccounts(transfer.getFromAccount(), transfer.getToAccount());
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

    private void rejectLoanAccounts(Account fromAccount, Account toAccount) {
        if (fromAccount.isLiability() || toAccount.isLiability()) {
            throw new IllegalArgumentException("Loan repayments are recorded in the Loans section, not as transfers");
        }
        if (fromAccount.isProperty() || toAccount.isProperty()) {
            throw new IllegalArgumentException("A property is not a cash account; update its value instead of transferring money");
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
        if (!isMember(transfer, user.getId())) {
            throw new SecurityException("You don't have access to this transfer");
        }
        // Zmienia autor albo właściciel grupy konta, z którego wyszły pieniądze.
        if (!transfer.getUser().getId().equals(user.getId())
                && !transfer.getFromAccount().getGroup().isOwnedBy(user.getId())) {
            throw new SecurityException("Only the author or the group owner can change this transfer");
        }
        return transfer;
    }
    /** Przelew widzą członkowie grupy któregokolwiek z jego kont. */
    private static boolean isMember(Transfer transfer, Long userId) {
        return isMember(transfer.getFromAccount(), userId) || isMember(transfer.getToAccount(), userId);
    }

    private static boolean isMember(Account account, Long userId) {
        return account.getGroup().getUsers().stream().anyMatch(u -> u.getId().equals(userId));
    }
}