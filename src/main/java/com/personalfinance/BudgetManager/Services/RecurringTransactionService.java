package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.*;
import com.personalfinance.BudgetManager.Exception.AccountException;
import com.personalfinance.BudgetManager.Exception.CategoryException;
import com.personalfinance.BudgetManager.Exception.ResourceNotFoundException;
import com.personalfinance.BudgetManager.Exception.SubcategoryException;
import com.personalfinance.BudgetManager.Exception.UserException;
import com.personalfinance.BudgetManager.Model.*;
import com.personalfinance.BudgetManager.Repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class RecurringTransactionService {

    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionService.class);
    /** Bezpiecznik: ile zaległych wystąpień jednego szablonu księgujemy automatycznie za jednym razem. */
    static final int MAX_CATCH_UP = 24;
    private static final int MAX_OCCURRENCES = 500;

    private final RecurringTransactionRepository recurringRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    public RecurringTransactionService(RecurringTransactionRepository recurringRepository,
                                       AccountRepository accountRepository,
                                       CategoryRepository categoryRepository,
                                       SubcategoryRepository subcategoryRepository,
                                       UserRepository userRepository,
                                       TransactionService transactionService) {
        this.recurringRepository = recurringRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
    }

    @Transactional(readOnly = true)
    public List<RecurringTransactionDTO> getAll(Long userId) {
        return recurringRepository.findAllForUser(userId).stream().map(RecurringTransactionService::toDTO).toList();
    }

    @Transactional
    public RecurringTransactionDTO create(Long userId, CreateRecurringTransactionRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(userId));
        requireIncomeOrExpense(request.type());
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("End date must not be before start date");
        }
        RecurringTransaction recurring = new RecurringTransaction();
        recurring.setUser(user);
        recurring.setType(request.type());
        recurring.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        recurring.setDescription(blankToNull(request.description()));
        recurring.setFrequency(request.frequency());
        recurring.setStartDate(request.startDate());
        recurring.setNextDate(request.startDate());
        recurring.setEndDate(request.endDate());
        recurring.setAutoPost(request.autoPost());
        recurring.setActive(true);
        recurring.setAccount(accessibleAccount(request.accountId(), userId));
        applyCategory(recurring, request.categoryId(), request.subcategoryId());
        return toDTO(recurringRepository.save(recurring));
    }

    @Transactional
    public RecurringTransactionDTO update(Long userId, Long id, UpdateRecurringTransactionRequest request) {
        RecurringTransaction recurring = owned(userId, id);
        if (request.amount() != null) recurring.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        if (request.description() != null) recurring.setDescription(blankToNull(request.description()));
        if (request.frequency() != null) recurring.setFrequency(request.frequency());
        if (request.nextDate() != null) {
            recurring.setNextDate(request.nextDate());
            recurring.setStartDate(request.nextDate());
        }
        if (Boolean.TRUE.equals(request.clearEndDate())) recurring.setEndDate(null);
        else if (request.endDate() != null) recurring.setEndDate(request.endDate());
        if (request.autoPost() != null) recurring.setAutoPost(request.autoPost());
        if (request.active() != null) recurring.setActive(request.active());
        if (request.accountId() != null) recurring.setAccount(accessibleAccount(request.accountId(), userId));
        boolean clearSubcategory = Boolean.TRUE.equals(request.clearSubcategory());
        if (clearSubcategory && request.subcategoryId() != null) {
            throw new IllegalArgumentException("Pass either subcategoryId or clearSubcategory, not both");
        }
        // Po zmianie konta kategoria jest sprawdzana jeszcze raz: musi pasować do grupy nowego konta.
        if (request.categoryId() != null || request.subcategoryId() != null || clearSubcategory
                || request.accountId() != null) {
            Long categoryId = request.categoryId() != null ? request.categoryId() : recurring.getCategory().getId();
            Long subcategoryId;
            if (clearSubcategory) {
                subcategoryId = null;
            } else if (request.subcategoryId() != null) {
                subcategoryId = request.subcategoryId();
            } else if (recurring.getSubcategory() != null
                    && recurring.getSubcategory().getCategory().getId().equals(categoryId)) {
                subcategoryId = recurring.getSubcategory().getId();
            } else {
                // nowa kategoria, a stara podkategoria do niej nie należy
                subcategoryId = null;
            }
            applyCategory(recurring, categoryId, subcategoryId);
        }
        if (recurring.getEndDate() != null && recurring.getEndDate().isBefore(recurring.getNextDate())) {
            throw new IllegalArgumentException("End date must not be before the next payment date");
        }
        return toDTO(recurring);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        recurringRepository.delete(owned(userId, id));
    }

    /** Księguje najbliższe wystąpienie jako zwykłą transakcję i przesuwa termin. */
    @Transactional
    public RecurringTransactionDTO post(Long userId, Long id, PostRecurringRequest request) {
        RecurringTransaction recurring = owned(userId, id);
        if (!recurring.isActive()) {
            throw new IllegalStateException("Recurring transaction is not active");
        }
        BigDecimal amount = request != null && request.amount() != null ? request.amount() : recurring.getAmount();
        LocalDate date = request != null && request.date() != null ? request.date() : recurring.getNextDate();
        createTransaction(recurring, amount, date);
        recurring.advance();
        return toDTO(recurring);
    }

    /** Pomija najbliższe wystąpienie bez tworzenia transakcji. */
    @Transactional
    public RecurringTransactionDTO skip(Long userId, Long id) {
        RecurringTransaction recurring = owned(userId, id);
        recurring.advance();
        return toDTO(recurring);
    }

    @Transactional(readOnly = true)
    public List<UpcomingPaymentDTO> upcoming(Long userId, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        LocalDate start = from != null ? from : today;
        LocalDate end = to != null ? to : start.plusDays(60);
        if (end.isBefore(start) || start.plusDays(400).isBefore(end)) {
            throw new IllegalArgumentException("Invalid date range (max 400 days)");
        }
        List<UpcomingPaymentDTO> result = new ArrayList<>();
        for (RecurringTransaction r : recurringRepository.findAllForUser(userId)) {
            if (!r.isActive()) continue;
            LocalDate date = r.getNextDate();
            boolean first = true;
            int guard = 0;
            while (!date.isAfter(end) && guard++ < MAX_OCCURRENCES
                    && (r.getEndDate() == null || !date.isAfter(r.getEndDate()))) {
                if (!date.isBefore(start)) {
                    result.add(new UpcomingPaymentDTO(r.getId(), date, r.getType(), r.getAmount(),
                            currency(r), label(r), r.getCategory().getName(), r.getCategory().getColor(),
                            r.getAccount().getName(), r.isAutoPost(), date.isBefore(today), first));
                }
                first = false;
                date = r.getFrequency().next(date, r.getStartDate());
            }
        }
        result.sort(Comparator.comparing(UpcomingPaymentDTO::date).thenComparing(UpcomingPaymentDTO::recurringId));
        return result;
    }

    @Transactional(readOnly = true)
    public List<Long> findDueForAutoPost(LocalDate date) {
        return recurringRepository.findDueForAutoPost(date).stream().map(RecurringTransaction::getId).toList();
    }

    /** Automatyczne księgowanie zaległych wystąpień jednego szablonu (osobna transakcja bazy). */
    @Transactional
    public int autoPost(Long id, LocalDate today) {
        RecurringTransaction recurring = recurringRepository.findById(id).orElse(null);
        if (recurring == null || !recurring.isActive() || !recurring.isAutoPost()) return 0;
        if (recurring.getAccount().getDeletedAt() != null) {
            log.info("Recurring transaction {} deactivated: account {} is archived", id, recurring.getAccount().getId());
            recurring.setActive(false);
            return 0;
        }
        int posted = 0;
        while (recurring.isActive() && !recurring.getNextDate().isAfter(today) && posted < MAX_CATCH_UP) {
            createTransaction(recurring, recurring.getAmount(), recurring.getNextDate());
            recurring.advance();
            posted++;
        }
        return posted;
    }

    @Transactional
    public void deactivate(Long id) {
        recurringRepository.findById(id).ifPresent(r -> r.setActive(false));
    }

    private void createTransaction(RecurringTransaction recurring, BigDecimal amount, LocalDate date) {
        CreateTransactionRequest tx = new CreateTransactionRequest();
        tx.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        tx.setType(recurring.getType());
        tx.setTransactionDate(date);
        tx.setDescription(label(recurring));
        tx.setAccountId(recurring.getAccount().getId());
        tx.setCategoryId(recurring.getCategory().getId());
        tx.setSubcategoryId(recurring.getSubcategory() != null ? recurring.getSubcategory().getId() : null);
        transactionService.createTransaction(tx, recurring.getUser().getEmail());
    }

    private RecurringTransaction owned(Long userId, Long id) {
        return recurringRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction", id));
    }

    private Account accessibleAccount(Long accountId, Long userId) {
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new AccountException(accountId));
        if (account.getDeletedAt() != null || account.getGroup().getUsers().stream()
                .noneMatch(member -> member.getId().equals(userId))) {
            throw new AccessDeniedException("No access to this account");
        }
        if (account.isLiability()) {
            throw new IllegalArgumentException("Recurring transactions cannot use a loan account");
        }
        if (account.isProperty()) {
            throw new IllegalArgumentException("Recurring transactions cannot use a property account");
        }
        return account;
    }


    private void applyCategory(RecurringTransaction recurring, Long categoryId, Long subcategoryId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new CategoryException(categoryId));
        Subcategory subcategory = subcategoryId == null ? null : subcategoryRepository.findById(subcategoryId)
                .orElseThrow(() -> new SubcategoryException(subcategoryId));
        if (category.getType() != recurring.getType()
                || (subcategory != null && !subcategory.getCategory().getId().equals(category.getId()))) {
            throw new IllegalArgumentException("Category, subcategory and transaction type must match");
        }
        UserGroup group = recurring.getAccount().getGroup();
        if (!category.isUsableIn(group) || (subcategory != null && !subcategory.isUsableIn(group))) {
            throw new IllegalArgumentException("This category belongs to another group than the account");
        }
        recurring.setCategory(category);
        recurring.setSubcategory(subcategory);
    }

    private static void requireIncomeOrExpense(CategoryType type) {
        if (type != CategoryType.INCOME && type != CategoryType.EXPENSE) {
            throw new IllegalArgumentException("Recurring transactions must be income or expense");
        }
    }

        private static String label(RecurringTransaction r) {
            if (r.getDescription() != null) return r.getDescription();
            return r.getSubcategory() != null ? r.getSubcategory().getName() : r.getCategory().getName();
        }

    private static String currency(RecurringTransaction r) {
        return r.getAccount().getCurrency() == null ? null : r.getAccount().getCurrency().toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static RecurringTransactionDTO toDTO(RecurringTransaction r) {
        BigDecimal monthly = r.getAmount().multiply(BigDecimal.valueOf(r.getFrequency().perMonth()))
                .setScale(2, RoundingMode.HALF_UP);
        return new RecurringTransactionDTO(r.getId(), r.getType(), r.getAmount(), r.getDescription(),
                r.getFrequency(), r.getStartDate(), r.getNextDate(), r.getEndDate(), r.isAutoPost(), r.isActive(),
                r.getAccount().getId(), r.getAccount().getName(), currency(r),
                r.getCategory().getId(), r.getCategory().getName(), r.getCategory().getColor(),
                r.getSubcategory() != null ? r.getSubcategory().getId() : null,
                r.getSubcategory() != null ? r.getSubcategory().getName() : null, monthly);
    }
}
