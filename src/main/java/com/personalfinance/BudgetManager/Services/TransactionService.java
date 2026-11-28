package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.CreateTransactionRequest;
import com.personalfinance.BudgetManager.DTO.CurrencyTotalsDTO;
import com.personalfinance.BudgetManager.DTO.TransactionFilter;
import com.personalfinance.BudgetManager.DTO.UpdateTransactionRequest;
import com.personalfinance.BudgetManager.Exception.*;
import com.personalfinance.BudgetManager.Model.*;
import com.personalfinance.BudgetManager.Repositories.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final AccountRepository accountRepository;
    private final UserService userService;

    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository, CategoryRepository categoryRepository, SubcategoryRepository subcategoryRepository, AccountRepository accountRepository, UserService userService) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.accountRepository = accountRepository;
        this.userService = userService;
    }

    @Transactional
    public Transaction createTransaction(CreateTransactionRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserException(userEmail));
        Account account = getAccessibleAccount(request.getAccountId(), user.getId());
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryException(request.getCategoryId()));
        Subcategory subcategory = findSubcategory(request.getSubcategoryId());
        validateCategory(category, subcategory, request.getType(), account);
        if (request.getType().isOpeningBalance()) {
            throw new IllegalArgumentException("Initial balance is created with the account");
        }
        if (account.isLiability() && request.getType() == CategoryType.INCOME) {
            throw new IllegalArgumentException("Income cannot be booked on a loan account; record a loan payment instead");
        }
        if (account.isProperty() && request.getType() != CategoryType.ADJUSTMENT) {
            throw new IllegalArgumentException("A property has no income or expenses; update its value instead");
        }
        requirePositive(request.getAmount());
        applyBalanceChange(account, request.getType(), request.getAmount());

        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : LocalDate.now());
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setSubcategory(subcategory);
        transaction.setAccount(account);

        return transactionRepository.save(transaction);
    }

    /** Największa strona listy transakcji. */
    static final int MAX_PAGE_SIZE = 200;
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("transactionDate"), Sort.Order.desc("id"));
    /** Strona transakcji i sumy dla całego filtra. */
    public record TransactionPage(Page<Transaction> page, List<CurrencyTotalsDTO> totals) {
    }

    /**
     * Pełna lista (pulpit, raporty we froncie). Bez groupId obejmuje konta wszystkich grup
     * użytkownika, a nie tylko jego wpisy; mine = true zostawia tylko wpisy zalogowanej osoby.
     */
    @Transactional(readOnly = true)
    public List<Transaction> getTransactions(
            Long groupId, String userEmail, Integer month, Integer year, CategoryType type, Long categoryId,
            boolean mine) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserException(userEmail));
        Specification<Transaction> spec = scope(user, groupId, mine);
        if (type != null) {
            spec = spec.and(TransactionSpecification.hasType(type));
        }
        if (categoryId != null) {
            spec = spec.and(TransactionSpecification.hasCategory(categoryId));
        }
        if (month != null && year != null) {
            spec = spec.and(TransactionSpecification.inMonth(month, year));
        }
        return transactionRepository.findAll(spec, NEWEST_FIRST);
    }

    /** Lista transakcji po stronach (od najnowszych) z filtrami po stronie serwera. */
    @Transactional(readOnly = true)
    public TransactionPage getTransactionPage(String userEmail, TransactionFilter filter, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE);
        }
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new IllegalArgumentException("The start date must not be after the end date");
        }
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserException(userEmail));
        Specification<Transaction> spec = scope(user, filter.groupId(), filter.mine());
        if (filter.from() != null) spec = spec.and(TransactionSpecification.from(filter.from()));
        if (filter.to() != null) spec = spec.and(TransactionSpecification.to(filter.to()));
        if (filter.type() != null) spec = spec.and(TransactionSpecification.hasType(filter.type()));
        if (filter.categoryId() != null) spec = spec.and(TransactionSpecification.hasCategory(filter.categoryId()));
        if (filter.accountId() != null) spec = spec.and(TransactionSpecification.hasAccount(filter.accountId()));
        if (filter.q() != null && !filter.q().isBlank()) spec = spec.and(TransactionSpecification.matches(filter.q()));

        Page<Transaction> result = transactionRepository.findAll(spec, PageRequest.of(page, size, NEWEST_FIRST));
        return new TransactionPage(result, transactionRepository.totalsByCurrency(spec));
    }

    /** Konta jednej grupy (użytkownik musi do niej należeć) albo wszystkich jego grup; opcjonalnie tylko jego wpisy. */
    private static Specification<Transaction> scope(User user, Long groupId, boolean mine) {
        if (groupId != null && user.getUserGroups().stream().noneMatch(g -> g.getId().equals(groupId))) {
            throw new AccessDeniedException("No access to this group");
        }
        Specification<Transaction> spec = groupId != null
                ? TransactionSpecification.belongsToGroup(groupId)
                : TransactionSpecification.inGroups(CategoryService.groupIds(user));
        return mine ? spec.and(TransactionSpecification.hasUser(user.getId())) : spec;
    }

    @Transactional
    public void deleteTransactionById(Long id) {

        Long userId = userService.getCurrentUser().getId();
        Transaction transaction = editableTransaction(id, userId);

        if (transaction.getType().isOpeningBalance()) {
            throw new IllegalArgumentException("Initial balance transaction cannot be deleted");
        }
        rejectLoanPaymentPart(id);
        Account account = getAccessibleAccount(transaction.getAccount().getId(), userId);
        applyBalanceChange(account, transaction.getType(), transaction.getAmount().negate());
        transactionRepository.delete(transaction);
    }

    @Transactional
    public Transaction updateTransaction(Long id, UpdateTransactionRequest request) {
        Long userId = userService.getCurrentUser().getId();
        Transaction transaction = editableTransaction(id, userId);
        if (transaction.getType().isOpeningBalance()) {
            throw new IllegalArgumentException("Initial balance transaction cannot be edited");
        }
        rejectLoanPaymentPart(id);
        Account account = getAccessibleAccount(transaction.getAccount().getId(), userId);
        Category category = request.getCategoryId() == null ? transaction.getCategory()
                : categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryException(request.getCategoryId()));
        boolean clearSubcategory = Boolean.TRUE.equals(request.getClearSubcategory());
        if (clearSubcategory && request.getSubcategoryId() != null) {
            throw new IllegalArgumentException("Pass either subcategoryId or clearSubcategory, not both");
        }
        Subcategory subcategory = resolveSubcategory(transaction.getSubcategory(), category,
                request.getSubcategoryId(), clearSubcategory);
        if (request.getCategoryId() != null || request.getSubcategoryId() != null) {
            validateCategory(category, subcategory, transaction.getType(), account);
        }
        if (request.getAmount() != null) {
            requirePositive(request.getAmount());
            applyBalanceChange(account, transaction.getType(), request.getAmount().subtract(transaction.getAmount()));
        }
        updateExistingTransactionWithPatch(transaction, request);
        transaction.setCategory(category);
        transaction.setSubcategory(subcategory);
        return transactionRepository.save(transaction);
    }

    /**
     * Transakcję zmienia lub usuwa jej autor albo właściciel grupy konta. Pozostali członkowie
     * grupy ją widzą, ale nie mogą jej zmienić; osoba spoza grupy dostaje ten sam błąd co dotąd.
     */
    private Transaction editableTransaction(Long id, Long userId) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new AccessDeniedException("No access to this transaction"));
        UserGroup group = transaction.getAccount().getGroup();
        boolean member = group.getUsers().stream().anyMatch(u -> u.getId().equals(userId));
        if (!member) {
            throw new AccessDeniedException("No access to this transaction");
        }
        if (!transaction.getUser().getId().equals(userId) && !group.isOwnedBy(userId)) {
            throw new AccessDeniedException("Only the author or the group owner can change this transaction");
        }
        return transaction;
    }

    /** Odsetki i opłaty ze spłaty kredytu zmienia się razem ze spłatą, inaczej rozjechałyby się salda. */
    private void rejectLoanPaymentPart(Long transactionId) {
        if (transactionRepository.isPartOfLoanPayment(transactionId)) {
            throw new ConflictException("This transaction is part of a loan payment; edit or delete the payment in the Loans section");
        }
    }

    private Account getAccessibleAccount(Long accountId, Long userId) {
        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new AccountException(accountId));
        if (account.getDeletedAt() != null || account.getGroup().getUsers().stream()
                .noneMatch(member -> member.getId().equals(userId))) {
            throw new AccessDeniedException("No access to this account");
        }
        return account;
    }

    private Subcategory findSubcategory(Long subcategoryId) {
        if (subcategoryId == null) {
            return null;
        }
        return subcategoryRepository.findById(subcategoryId)
                .orElseThrow(() -> new SubcategoryException(subcategoryId));
    }

    /** Po zmianie kategorii stara podkategoria, która do niej nie pasuje, jest zdejmowana. */
    private Subcategory resolveSubcategory(Subcategory current, Category category, Long subcategoryId, boolean clear) {
        if (clear) {
            return null;
        }
        if (subcategoryId != null) {
            return findSubcategory(subcategoryId);
        }
        if (current != null && !current.getCategory().getId().equals(category.getId())) {
            return null;
        }
        return current;
    }

    /**
     * Typ kategorii musi pasować do transakcji; podkategoria (jeśli jest) musi należeć do kategorii;
     * obie muszą być systemowe albo z grupy konta.
     */
    private void validateCategory(Category category, Subcategory subcategory, CategoryType type, Account account) {
        if (category.getType() != type
                || (subcategory != null && !subcategory.getCategory().getId().equals(category.getId()))) {
            throw new IllegalArgumentException("Category, subcategory and transaction type must match");
        }
        if (!category.isUsableIn(account.getGroup())
                || (subcategory != null && !subcategory.isUsableIn(account.getGroup()))) {
            throw new IllegalArgumentException("This category belongs to another group than the account");
        }
    }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }


private void applyBalanceChange(Account account, CategoryType type, BigDecimal amount) {
    BigDecimal delta = type.signed(amount);
    BigDecimal after = account.getBalance().add(delta);
    if (account.isLiability() && after.signum() > 0) {
        throw new IllegalArgumentException("Loan balance cannot become positive (remaining debt: "
                + account.getBalance().negate() + ")");
    }
    account.setBalance(after);
}

private void updateExistingTransactionWithPatch(Transaction existingTransaction, UpdateTransactionRequest request) {
        if (request.getAmount() != null) existingTransaction.setAmount(request.getAmount());
        if (request.getDescription() != null) existingTransaction.setDescription(request.getDescription());
        if (request.getTransactionDate() != null) existingTransaction.setTransactionDate(request.getTransactionDate());

    }

}
