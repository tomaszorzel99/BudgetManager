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
 * Tu zakładanie, lista i zmiana parametrów; spłaty są w {@link LoanPaymentService}.
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
    private final LoanPaymentRepository paymentRepository;

    public LoanService(LoanRepository loanRepository, AccountRepository accountRepository,
                       UserRepository userRepository, TransactionRepository transactionRepository,
                       CategoryRepository categoryRepository, SubcategoryRepository subcategoryRepository,
                       LoanPaymentRepository paymentRepository) {
        this.loanRepository = loanRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.paymentRepository = paymentRepository;
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
        setSubsidy(loan, request.subsidizedRate(), request.subsidyEndDate());
        loan = loanRepository.save(loan);

        createOpeningTransaction(account, outstanding, balanceDate, user);
        return toDTO(loan, today, LoanPaymentRepository.Totals.EMPTY);
    }

    @Transactional(readOnly = true)
    public List<LoanDTO> getLoans(Long userId) {
        LocalDate today = LocalDate.now();
        return loanRepository.findActiveForUser(userId).stream().map(l -> dto(l, today)).toList();
    }

    @Transactional(readOnly = true)
    public LoanDTO getLoan(Long userId, Long id) {
        return dto(owned(userId, id), LocalDate.now());
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
        if (Boolean.TRUE.equals(request.clearSubsidy())) {
            loan.setSubsidizedRate(null);
            loan.setSubsidyEndDate(null);
        } else if (request.subsidizedRate() != null || request.subsidyEndDate() != null) {
            setSubsidy(loan,
                    request.subsidizedRate() != null ? request.subsidizedRate() : loan.getSubsidizedRate(),
                    request.subsidyEndDate() != null ? request.subsidyEndDate() : loan.getSubsidyEndDate());
        } else if (loan.getSubsidizedRate() != null
                && loan.getSubsidizedRate().compareTo(loan.getAnnualInterestRate()) > 0) {
            throw new IllegalArgumentException("Subsidized rate cannot be higher than the loan interest rate");
        }
        return dto(loan, LocalDate.now());
    }

    /** Dopłata do rat: oprocentowanie po dopłacie nie wyższe niż z umowy; data końca tylko razem z nim. */
    private static void setSubsidy(Loan loan, BigDecimal subsidizedRate, LocalDate endDate) {
        if (subsidizedRate == null) {
            if (endDate != null) {
                throw new IllegalArgumentException("Subsidy end date requires the subsidized interest rate");
            }
            return;
        }
        BigDecimal value = rate(subsidizedRate);
        if (value.compareTo(loan.getAnnualInterestRate()) > 0) {
            throw new IllegalArgumentException("Subsidized rate cannot be higher than the loan interest rate");
        }
        if (endDate != null && endDate.isBefore(loan.getStartDate())) {
            throw new IllegalArgumentException("Subsidy end date cannot be before the loan start date");
        }
        loan.setSubsidizedRate(value);
        loan.setSubsidyEndDate(endDate);
    }

    private LoanDTO dto(Loan loan, LocalDate today) {
        return toDTO(loan, today, paymentRepository.totalsForLoan(loan.getId()));
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
        if (account.isProperty()) {
            throw new IllegalArgumentException("A loan cannot be repaid from a property account");
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

    static LoanDTO toDTO(Loan loan, LocalDate today, LoanPaymentRepository.Totals totals) {
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
                loan.getPaymentDay(), paidOff ? null : loan.nextPaymentDate(nextFrom(today, totals.lastInstallmentDate())),
                loan.getOverpaymentStrategy(),
                payer != null ? payer.getId() : null, payer != null ? payer.getName() : null,
                loan.getCreatedAt(),
                loan.getSubsidizedRate(), loan.getSubsidyEndDate(),
                totals.installmentPrincipal(), totals.overpaidPrincipal(),
                totals.interest().subtract(totals.subsidy()), totals.subsidy(), totals.fees(),
                totals.count(), totals.lastPaymentDate());
    }

    /** Następna rata liczy się od dziś, a gdy rata na ten termin jest już zapisana, od dnia po niej. */
    private static LocalDate nextFrom(LocalDate today, LocalDate lastInstallment) {
        return lastInstallment != null && !lastInstallment.isBefore(today) ? lastInstallment.plusDays(1) : today;
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