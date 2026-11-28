package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.BalanceAtDateDTO;
import com.personalfinance.BudgetManager.Model.*;
import com.personalfinance.BudgetManager.Repositories.AccountRepository;
import com.personalfinance.BudgetManager.Repositories.LoanRepository;
import com.personalfinance.BudgetManager.Repositories.TransactionRepository;
import com.personalfinance.BudgetManager.Repositories.TransferRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class HistoricalBalanceService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private  final TransferRepository transferRepository;
    private final LoanRepository loanRepository;

    public HistoricalBalanceService(AccountRepository accountRepository, TransactionRepository transactionRepository,
                                    TransferRepository transferRepository, LoanRepository loanRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transferRepository = transferRepository;
        this.loanRepository = loanRepository;
    }

    public BalanceAtDateDTO calculate(Collection<Long> groupIds, LocalDate date){
        List<Account> accounts = accountRepository.findForHistoricalReport(groupIds);

        if(accounts.isEmpty()){
            return new BalanceAtDateDTO(
                    date, null, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        List<Long> ids = accounts.stream().map(Account::getId).toList();
        Map<Long, BigDecimal> balances = new HashMap<>();


        for (Long id : ids) {
            balances.put(id, BigDecimal.ZERO);
        }

        for (Transaction transaction :
                transactionRepository.findForHistoricalReport(ids, date)) {

            BigDecimal amount = transaction.signedAmount();

            balances.merge(
                    transaction.getAccount().getId(),
                    amount,
                    BigDecimal::add);
        }

        for (Transfer transfer :
                transferRepository.findForHistoricalReport(ids, date)) {

            Long fromId = transfer.getFromAccount().getId();
            Long toId = transfer.getToAccount().getId();

            if (balances.containsKey(fromId)) {
                balances.merge(
                        fromId,
                        transfer.getAmount().negate(),
                        BigDecimal::add);
            }

            if (balances.containsKey(toId)) {
                balances.merge(
                        toId,
                        transfer.getReceivedAmount(),
                        BigDecimal::add);
            }
        }

        addEstimatedLoanDebt(ids, date, balances);

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal savings = BigDecimal.ZERO;
        BigDecimal investments = BigDecimal.ZERO;
        BigDecimal liabilities = BigDecimal.ZERO;
        BigDecimal property = BigDecimal.ZERO;
        String currency = null;

        for (Account account : accounts) {
            BigDecimal balance = balances.get(account.getId());

            if (balance.signum() != 0) {
                String accountCurrency =
                        account.getCurrency().toUpperCase(Locale.ROOT);

                if (currency != null && !currency.equals(accountCurrency)) {
                    throw new IllegalStateException(
                            "Historical report cannot sum accounts " +
                                    "in different currencies");
                }
                currency = accountCurrency;
            }

            total = total.add(balance);

            if (account.getAccountType() == AccountType.SAVINGS) {
                savings = savings.add(balance);
            } else if (account.getAccountType() == AccountType.INVESTMENT) {
                investments = investments.add(balance);
            } else if (account.isLiability()) {
                liabilities = liabilities.add(balance);
            } else if (account.isProperty()) {
                property = property.add(balance);
            }
        }

        return new BalanceAtDateDTO(
                date, currency, total, savings, investments, liabilities, property);
    }

    private void addEstimatedLoanDebt(List<Long> accountIds, LocalDate date, Map<Long, BigDecimal> balances) {
        List<Loan> loans = loanRepository.findByAccountIds(accountIds).stream()
                .filter(l -> !date.isBefore(l.getStartDate()))
                .toList();
        if (loans.isEmpty()) {
            return;
        }
        List<Long> loanAccountIds = loans.stream().map(l -> l.getAccount().getId()).toList();
        Map<Long, Transaction> openings = new HashMap<>();
        for (Transaction opening : transactionRepository.findLiabilityOpenings(loanAccountIds)) {
            openings.merge(opening.getAccount().getId(), opening,
                    (a, b) -> a.getTransactionDate().isBefore(b.getTransactionDate()) ? a : b);
        }
        for (Loan loan : loans) {
            Long accountId = loan.getAccount().getId();
            Transaction opening = openings.get(accountId);
            if (opening == null || !date.isBefore(opening.getTransactionDate())) {
                continue;
            }
            BigDecimal debt = LoanCalculator.estimatedOutstanding(
                    loan, opening.getTransactionDate(), opening.getAmount(), date);
            balances.merge(accountId, debt.negate(), BigDecimal::add);
        }
    }
}