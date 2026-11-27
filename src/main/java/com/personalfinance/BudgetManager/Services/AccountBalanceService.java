package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.Exception.AccountException;
import com.personalfinance.BudgetManager.Model.Account;
import com.personalfinance.BudgetManager.Model.Transfer;
import com.personalfinance.BudgetManager.Repositories.AccountRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

//import static com.personalfinance.BudgetManager.Exception.GlobalExceptionHandler.log;
import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@Service
@Slf4j
public class AccountBalanceService {

    private final AccountRepository accountRepository;

    public AccountBalanceService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void transfer(Long fromAccountId, Long toAccountId,
                         BigDecimal sourceAmount, BigDecimal receivedAmount) {
        validateAmounts(fromAccountId, toAccountId, sourceAmount, receivedAmount);
        Map<Long, BigDecimal> changes = new HashMap<>();
        add(changes, fromAccountId, sourceAmount.negate());
        add(changes, toAccountId, receivedAmount);
        applyChanges(changes, fromAccountId, toAccountId);
        log.info("Transferred {} from account {} and {} to account {}",
                sourceAmount, fromAccountId, receivedAmount, toAccountId);
    }

    public void replaceTransfer(Transfer original, Long newFromId, Long newToId,
                                BigDecimal newSourceAmount, BigDecimal newReceivedAmount) {
        validateAmounts(newFromId, newToId, newSourceAmount, newReceivedAmount);
        Map<Long, BigDecimal> changes = new HashMap<>();
        add(changes, original.getFromAccount().getId(), original.getAmount());
        add(changes, original.getToAccount().getId(), original.getReceivedAmount().negate());
        add(changes, newFromId, newSourceAmount.negate());
        add(changes, newToId, newReceivedAmount);
        applyChanges(changes, newFromId, newToId);
    }

    private void validateAmounts(Long fromId, Long toId, BigDecimal source, BigDecimal received) {
        if (fromId == null || toId == null || fromId.equals(toId)
                || source == null || source.signum() <= 0
                || received == null || received.signum() <= 0) {
            throw new IllegalArgumentException("Transfer requires distinct accounts and positive amounts");
        }
    }

    private void add(Map<Long, BigDecimal> changes, Long id, BigDecimal amount) {
        changes.merge(id, amount, BigDecimal::add);
    }

    private void applyChanges(Map<Long, BigDecimal> changes, Long newFromId, Long newToId) {
        Map<Long, Account> accounts = lockAll(changes);
        Account from = accounts.get(newFromId);
        Account to = accounts.get(newToId);
        if (from.getDeletedAt() != null || to.getDeletedAt() != null) {
            throw new IllegalArgumentException("Cannot transfer to or from an archived account");
        }
        checkAndApply(changes, accounts);
    }

    /**
     * Zmienia salda kilku kont naraz (np. spłata kredytu: konto płatnika i konto kredytu).
     * Blokuje konta w stałej kolejności, sprawdza reguły salda dla wszystkich i dopiero wtedy
     * zapisuje, więc albo zmieniają się wszystkie salda, albo żadne. Zera są pomijane.
     */
    @Transactional
    public Map<Long, Account> applyDeltas(Map<Long, BigDecimal> changes) {
        Map<Long, BigDecimal> nonZero = new HashMap<>();
        changes.forEach((id, delta) -> {
            if (delta.signum() != 0) nonZero.put(id, delta);
        });
        Map<Long, Account> accounts = lockAll(nonZero);
        checkAndApply(nonZero, accounts);
        return accounts;
    }

    private Map<Long, Account> lockAll(Map<Long, BigDecimal> changes) {
        Map<Long, Account> accounts = new HashMap<>();
        // Lock in a stable order, including every account touched by an edit.
        for (Long id : new TreeSet<>(changes.keySet())) {
            accounts.put(id, accountRepository.findByIdWithLock(id)
                    .orElseThrow(() -> new AccountException(id)));
        }
        return accounts;
    }

    private void checkAndApply(Map<Long, BigDecimal> changes, Map<Long, Account> accounts) {
        for (Map.Entry<Long, BigDecimal> entry : changes.entrySet()) {
            Account account = accounts.get(entry.getKey());
            BigDecimal delta = entry.getValue();
            if (account.getDeletedAt() != null && delta.signum() != 0) {
                throw new IllegalArgumentException("Cannot change an archived account balance");
            }
            checkBalanceAfter(account, delta);
        }
        for (Map.Entry<Long, BigDecimal> entry : changes.entrySet()) {
            Account account = accounts.get(entry.getKey());
            account.setBalance(account.getBalance().add(entry.getValue()));
            accountRepository.save(account);
        }
    }
    /**
     * Konto zwykłe nie może zejść poniżej zera, a konto kredytu powyżej zera
     * (nie da się spłacić więcej, niż wynosi dług). Cofnięcie spłaty kredytu jest dozwolone.
     */
    static void checkBalanceAfter(Account account, BigDecimal delta) {
        BigDecimal after = account.getBalance().add(delta);
        if (account.isLiability()) {
            if (delta.signum() > 0 && after.signum() > 0) {
                throw new IllegalArgumentException("Amount exceeds the remaining debt on " + account.getName()
                        + " (" + account.getBalance().negate() + ")");
            }
        } else if (delta.signum() < 0 && after.signum() < 0) {
            throw new IllegalArgumentException("Not enough balance in account " + account.getName());
        }
    }

//        // Keep the lock order stable for concurrent transfers in opposite directions.
//        Account first = accountRepository.findByIdWithLock(Math.min(fromAccountId, toAccountId))
//                .orElseThrow(() -> new AccountException(Math.min(fromAccountId, toAccountId)));
//        Account second = accountRepository.findByIdWithLock(Math.max(fromAccountId, toAccountId))
//                .orElseThrow(() -> new AccountException(Math.max(fromAccountId, toAccountId)));
//        Account fromAccount = fromAccountId < toAccountId ? first : second;
//        Account toAccount = fromAccountId < toAccountId ? second : first;
//
//        if (fromAccount.getDeletedAt() != null || toAccount.getDeletedAt() != null) {
//            throw new IllegalArgumentException("Cannot transfer to or from an archived account");
//        }
//        if (!fromAccount.getCurrency().equalsIgnoreCase(toAccount.getCurrency())) {
//            throw new IllegalArgumentException("Transfers between different currencies require an exchange rate");
//        }

//        if(fromAccount.getBalance().compareTo(amount) < 0){
//            throw new IllegalArgumentException("Not enough balance in the account");
//        }

//        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
//        toAccount.setBalance(toAccount.getBalance().add(amount));

//        accountRepository.save(fromAccount);
//        accountRepository.save(toAccount);

    //        log.info("Transferred {} from account {} to account {}",
//                amount, fromAccountId, toAccountId);
//    }
    @Transactional
    public void addIncome(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findByIdWithLock(accountId).orElseThrow(() -> new AccountException(accountId));
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
    }

//    public void addIncome(Long accountId, BigDecimal amount) {
//        Account account = accountRepository.findByIdWithLock(accountId).orElseThrow(() -> new AccountException(accountId));
//        account.setBalance(account.getBalance().add(amount));
//        accountRepository.save(account);
//    }

    @Transactional
    public void addExpense(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findByIdWithLock(accountId).orElseThrow(() -> new AccountException(accountId));
        account.setBalance(account.getBalance().subtract(amount));
    }

//    public void addExpense(Long accountId, BigDecimal amount) {
//        Account account = accountRepository.findByIdWithLock(accountId).orElseThrow(() -> new AccountException(accountId));
//        account.setBalance(account.getBalance().subtract(amount));
//        accountRepository.save(account);
//    }
}