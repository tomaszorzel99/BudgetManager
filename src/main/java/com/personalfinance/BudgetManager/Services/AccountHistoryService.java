package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.AccountHistoryDTO;
import com.personalfinance.BudgetManager.DTO.AccountHistoryEntryDTO;
import com.personalfinance.BudgetManager.Exception.AccountException;
import com.personalfinance.BudgetManager.Model.*;
import com.personalfinance.BudgetManager.Repositories.AccountRepository;
import com.personalfinance.BudgetManager.Repositories.TransactionRepository;
import com.personalfinance.BudgetManager.Repositories.TransferRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AccountHistoryService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;

    public AccountHistoryService(AccountRepository accountRepository, TransactionRepository transactionRepository,
                                 TransferRepository transferRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transferRepository = transferRepository;
    }

    /** Wszystkie operacje na koncie (wszystkich członków grupy) z saldem narastającym. */
    @Transactional(readOnly = true)
    public AccountHistoryDTO getHistory(Long accountId, Long userId) {
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new AccountException(accountId));
        if (account.getGroup().getUsers().stream().noneMatch(u -> u.getId().equals(userId))) {
            throw new AccessDeniedException("No access to this account");
        }

        record Row(LocalDate date, LocalDateTime created, long id, AccountHistoryEntryDTO entry) { }
        List<Row> rows = new ArrayList<>();
        for (Transaction t : transactionRepository.findAllForAccountHistory(accountId)) {
            BigDecimal amount = t.getType() == CategoryType.EXPENSE ? t.getAmount().negate() : t.getAmount();
            rows.add(new Row(t.getTransactionDate(), t.getCreatedAt(), t.getId(), new AccountHistoryEntryDTO(
                    "TRANSACTION", t.getId(), t.getTransactionDate(), t.getType().name(), t.getDescription(),
                    t.getCategory().getName(), t.getSubcategory().getName(), t.getCategory().getColor(), null,
                    t.getUser() != null ? t.getUser().getName() : null, amount, null)));
        }
        for (Transfer t : transferRepository.findAllForAccountHistory(accountId)) {
            boolean outgoing = t.getFromAccount().getId().equals(accountId);
            BigDecimal amount = outgoing ? t.getAmount().negate() : t.getReceivedAmount();
            String counterparty = outgoing ? t.getToAccount().getName() : t.getFromAccount().getName();
            rows.add(new Row(t.getTransferDate(), t.getCreatedAt(), t.getId(), new AccountHistoryEntryDTO(
                    outgoing ? "TRANSFER_OUT" : "TRANSFER_IN", t.getId(), t.getTransferDate(), "TRANSFER",
                    t.getDescription(), null, null, null, counterparty,
                    t.getUser() != null ? t.getUser().getName() : null, amount, null)));
        }
        rows.sort(Comparator.comparing(Row::date)
                .thenComparing(Row::created, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparingLong(Row::id));

        BigDecimal sum = rows.stream().map(r -> r.entry().amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal current = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
        BigDecimal opening = current.subtract(sum);

        BigDecimal running = opening;
        List<AccountHistoryEntryDTO> entries = new ArrayList<>(rows.size());
        for (Row row : rows) {
            AccountHistoryEntryDTO e = row.entry();
            running = running.add(e.amount());
            entries.add(new AccountHistoryEntryDTO(e.kind(), e.id(), e.date(), e.type(), e.description(),
                    e.categoryName(), e.subcategoryName(), e.categoryColor(), e.counterparty(), e.userName(),
                    e.amount(), running));
        }
        return new AccountHistoryDTO(account.getId(), account.getName(), account.getCurrency(), current, opening, entries);
    }
}
