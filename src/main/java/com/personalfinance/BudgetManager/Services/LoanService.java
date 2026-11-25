package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.CreateLoanRequest;
import com.personalfinance.BudgetManager.DTO.LoanDTO;
import com.personalfinance.BudgetManager.DTO.UpdateLoanRequest;
import com.personalfinance.BudgetManager.Exception.AccountException;
import com.personalfinance.BudgetManager.Exception.ResourceNotFoundException;
import com.personalfinance.BudgetManager.Exception.UserException;
import com.personalfinance.BudgetManager.Model.*;
import com.personalfinance.BudgetManager.Repositories.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Kredyty: konto LOAN z ujemnym saldem plus parametry z umowy ({@link Loan}).
 * Spłaty dochodzą w etapie 2; tu zakładanie, lista i zmiana parametrów.
 */
@Service
public class LoanService {

    static final String OPENING_CATEGORY = "Initial Balance";
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final LoanRepository loanRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;

    public LoanService(LoanRepository loanRepository, AccountRepository accountRepository,
                       UserRepository userRepository, TransactionRepository transactionRepository,
                       CategoryRepository categoryRepository, SubcategoryRepository subcategoryRepository) {
        this.loanRepository = loanRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
    }

    /** Zakłada konto LOAN z saldem −pozostało do spłaty, parametry kredytu i transakcję salda początkowego długu. */
    @Transactional
    public LoanDTO create(Long userId, CreateLoanRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(userId));
        UserGroup group = user.getUserGroups().stream()
                .filter(g -> g.getId().equals(request.groupId()))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("User does not belong to the specified group"));

        LocalDate today = LocalDate.now();
        LocalDate balanceDate = request.balanceDate() != null ? request.balanceDate() : today;
        if (request.startDate().isAfter(today)) {
            throw new IllegalArgumentException("Loan start date cannot be in the future");
        }
        if (balanceDate.isAfter(today) || balanceDate.isBefore(request.startDate())) {
            throw new IllegalArgumentException("Balance date must be between the loan start date and today");
        }
        String currency = request.currency().toUpperCase(Locale.ROOT);
        BigDecimal outstanding = money(request.outstandingPrincipal());

        Account account = new Account();
        account.setName(request.name().trim());
        account.setCurrency(currency);
        account.setAccountType(AccountType.LOAN);
        account.setAvailableForSpending(false);
        account.setGroup(group);
        account.setBalance(outstanding.negate());
        account = accountRepository.save(account);

        Loan loan = new Loan();
        loan.setAccount(account);
        loan.setLender(blankToNull(request.lender()));
        loan.setOriginalPrincipal(money(request.originalPrincipal()));
        loan.setStartDate(request.startDate());
        loan.setTermMonths(request.termMonths());
        loan.setAnnualInterestRate(rate(request.annualInterestRate()));
        loan.setRateType(request.rateType() != null ? request.rateType() : LoanRateType.FIXED);
        loan.setInstallmentType(request.installmentType());
        loan.setPaymentDay(request.paymentDay());
        loan.setOverpaymentStrategy(request.overpaymentStrategy() != null
                ? request.overpaymentStrategy() : OverpaymentStrategy.SHORTEN_TERM);
        if (request.defaultPaymentAccountId() != null) {
            loan.setDefaultPaymentAccount(paymentAccount(request.defaultPaymentAccountId(), userId, currency));
        }
        loan = loanRepository.save(loan);

        createOpeningTransaction(account, outstanding, balanceDate, user);
        return toDTO(loan, today);
    }

    @Transactional(readOnly = true)
    public List<LoanDTO> getLoans(Long userId) {
        LocalDate today = LocalDate.now();
        return loanRepository.findActiveForUser(userId).stream().map(l -> toDTO(l, today)).toList();
    }

    @Transactional(readOnly = true)
    public LoanDTO getLoan(Long userId, Long id) {
        return toDTO(owned(userId, id), LocalDate.now());
    }

    @Transactional
    public LoanDTO update(Long userId, Long id, UpdateLoanRequest request) {
        Loan loan = owned(userId, id);
        Account account = loan.getAccount();
        if (account.getDeletedAt() != null) {
            throw new IllegalStateException("Loan account is archived");
        }
        if (request.name() != null) {
            if (request.name().isBlank()) throw new IllegalArgumentException("Name cannot be blank");
            account.setName(request.name().trim());
        }
        if (request.lender() != null) loan.setLender(blankToNull(request.lender()));
        if (request.termMonths() != null) loan.setTermMonths(request.termMonths());
        if (request.annualInterestRate() != null) loan.setAnnualInterestRate(rate(request.annualInterestRate()));
        if (request.rateType() != null) loan.setRateType(request.rateType());
        if (request.paymentDay() != null) loan.setPaymentDay(request.paymentDay());
        if (request.overpaymentStrategy() != null) loan.setOverpaymentStrategy(request.overpaymentStrategy());
        if (Boolean.TRUE.equals(request.clearDefaultPaymentAccount())) {
            loan.setDefaultPaymentAccount(null);
        } else if (request.defaultPaymentAccountId() != null) {
            loan.setDefaultPaymentAccount(paymentAccount(request.defaultPaymentAccountId(), userId, account.getCurrency()));
        }
        return toDTO(loan, LocalDate.now());
    }

    private Loan owned(Long userId, Long id) {
        return loanRepository.findByIdForUser(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));
    }

    /** Konto, z którego domyślnie idą raty: dostępne dla użytkownika, aktywne, nie kredyt, ta sama waluta. */
    private Account paymentAccount(Long accountId, Long userId, String currency) {
        Account account = accountRepository.findByIdAndGroupUsersId(accountId, userId)
                .orElseThrow(() -> new AccountException("Account not found or access denied"));
        if (account.getDeletedAt() != null) {
            throw new IllegalArgumentException("Payment account is archived");
        }
        if (account.isLiability()) {
            throw new IllegalArgumentException("A loan cannot be repaid from another loan account");
        }
        if (!account.getCurrency().equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("Payment account must use the loan currency (" + currency + ")");
        }
        return account;
    }

    private void createOpeningTransaction(Account account, BigDecimal amount, LocalDate date, User user) {
        Category category = categoryRepository.findFirstByName(OPENING_CATEGORY)
                .orElseThrow(() -> new IllegalStateException("Category '" + OPENING_CATEGORY + "' not found"));
        Subcategory subcategory = subcategoryRepository.findByCategoryName(OPENING_CATEGORY).stream()
                .findFirst().orElse(null);
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setType(CategoryType.LIABILITY_OPENING);
        transaction.setDescription("Saldo początkowe kredytu");
        transaction.setTransactionDate(date);
        transaction.setCategory(category);
        transaction.setSubcategory(subcategory);
        transaction.setAccount(account);
        transaction.setUser(user);
        transactionRepository.save(transaction);
    }

    static LoanDTO toDTO(Loan loan, LocalDate today) {
        Account account = loan.getAccount();
        BigDecimal outstanding = account.getBalance() == null ? BigDecimal.ZERO : account.getBalance().negate();
        BigDecimal repaid = loan.getOriginalPrincipal().subtract(outstanding).max(BigDecimal.ZERO);
        BigDecimal percent = repaid.multiply(HUNDRED)
                .divide(loan.getOriginalPrincipal(), 1, RoundingMode.HALF_UP)
                .min(HUNDRED);
        Account payer = loan.getDefaultPaymentAccount();
        boolean paidOff = outstanding.signum() == 0;
        return new LoanDTO(
                loan.getId(), account.getId(), account.getName(),
                account.getGroup().getId(), account.getGroup().getName(), account.getCurrency(),
                loan.getLender(), loan.getOriginalPrincipal(), outstanding, repaid, percent,
                loan.getStartDate(), loan.getTermMonths(), loan.plannedEndDate(),
                loan.getAnnualInterestRate(), loan.getRateType(), loan.getInstallmentType(),
                loan.getPaymentDay(), paidOff ? null : loan.nextPaymentDate(today),
                loan.getOverpaymentStrategy(),
                payer != null ? payer.getId() : null, payer != null ? payer.getName() : null,
                loan.getCreatedAt());
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal rate(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}