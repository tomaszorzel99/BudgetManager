package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.LoanPaymentDTO;
import com.personalfinance.BudgetManager.DTO.LoanPaymentRequest;
import com.personalfinance.BudgetManager.DTO.LoanPaymentSuggestionDTO;
import com.personalfinance.BudgetManager.DTO.LoanDTO;
import com.personalfinance.BudgetManager.DTO.ReconcileLoanRequest;
import com.personalfinance.BudgetManager.Exception.AccountException;
import com.personalfinance.BudgetManager.Exception.ResourceNotFoundException;
import com.personalfinance.BudgetManager.Exception.UserException;
import com.personalfinance.BudgetManager.Model.*;
import com.personalfinance.BudgetManager.Repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spłaty kredytu. Jedna spłata to w jednej transakcji bazy: przelew kapitału z konta płatnika
 * na konto kredytu, wydatek na odsetki (po dopłacie) i wydatek na opłaty, oraz wiersz
 * {@link LoanPayment}, który je łączy. Edycja i usunięcie też idą tędy, żeby części się nie rozjechały.
 */
@Service
public class LoanPaymentService {

    private final LoanRepository loanRepository;
    private final LoanPaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceService accountBalanceService;
    private final TransferRepository transferRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final UserRepository userRepository;
    private final LoanService loanService;

    public LoanPaymentService(LoanRepository loanRepository, LoanPaymentRepository paymentRepository,
                              AccountRepository accountRepository, AccountBalanceService accountBalanceService,
                              TransferRepository transferRepository, TransactionRepository transactionRepository,
                              CategoryRepository categoryRepository, SubcategoryRepository subcategoryRepository,
                              UserRepository userRepository, LoanService loanService) {
        this.loanRepository = loanRepository;
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
        this.accountBalanceService = accountBalanceService;
        this.transferRepository = transferRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.userRepository = userRepository;
        this.loanService = loanService;
    }

    // ---------------------------------------------------------------- zapis, edycja, usunięcie

    @Transactional
    public LoanPaymentDTO record(Long userId, Long loanId, LoanPaymentRequest request) {
        Loan loan = activeLoan(userId, loanId);
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(userId));
        Amounts amounts = Amounts.of(request, loan);
        Account payer = payerAccount(request.payerAccountId(), userId, loan);

        Map<Long, BigDecimal> deltas = new HashMap<>();
        addDeltas(deltas, loan, payer, request.kind(), amounts, 1);
        accountBalanceService.applyDeltas(deltas);

        LoanPayment payment = new LoanPayment();
        payment.setLoan(loan);
        payment.setUser(user);
        fill(payment, request, amounts, payer, loan);
        syncParts(payment, user);
        payment = paymentRepository.save(payment);
        return toDTO(payment, outstanding(loan));
    }

    @Transactional
    public LoanPaymentDTO update(Long userId, Long loanId, Long paymentId, LoanPaymentRequest request) {
        Loan loan = activeLoan(userId, loanId);
        LoanPayment payment = payment(loan, paymentId);
        Amounts amounts = Amounts.of(request, loan);
        Account payer = payerAccount(request.payerAccountId(), userId, loan);

        // Cofnięcie starej spłaty i zapis nowej jako jedna zmiana sald: reguły sprawdzane są na wyniku.
        Map<Long, BigDecimal> deltas = new HashMap<>();
        addDeltas(deltas, loan, payment.getPayerAccount(), payment.getKind(), Amounts.of(payment), -1);
        addDeltas(deltas, loan, payer, request.kind(), amounts, 1);
        accountBalanceService.applyDeltas(deltas);

        fill(payment, request, amounts, payer, loan);
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(userId));
        syncParts(payment, user);
        return toDTO(paymentRepository.save(payment), null);
    }

    @Transactional
    public void delete(Long userId, Long loanId, Long paymentId) {
        Loan loan = activeLoan(userId, loanId);
        LoanPayment payment = payment(loan, paymentId);

        Map<Long, BigDecimal> deltas = new HashMap<>();
        addDeltas(deltas, loan, payment.getPayerAccount(), payment.getKind(), Amounts.of(payment), -1);
        accountBalanceService.applyDeltas(deltas);

        Transfer transfer = payment.getTransfer();
        Transaction interest = payment.getInterestTransaction();
        Transaction fee = payment.getFeeTransaction();
        paymentRepository.delete(payment);
        paymentRepository.flush(); // najpierw wiersz spłaty, bo wskazuje na przelew i wydatki
        if (transfer != null) transferRepository.delete(transfer);
        if (interest != null) transactionRepository.delete(interest);
        if (fee != null) transactionRepository.delete(fee);
    }

    @Transactional(readOnly = true)
    public List<LoanPaymentDTO> list(Long userId, Long loanId, LocalDate from, LocalDate to) {
        Loan loan = ownedLoan(userId, loanId);
        // Dług po każdej spłacie: od dzisiejszego salda wstecz, od najnowszej spłaty.
        BigDecimal after = outstanding(loan);
        List<LoanPaymentDTO> result = new ArrayList<>();
        for (LoanPayment p : paymentRepository.findForLoan(loanId, null, null)) {
            if ((from == null || !p.getPaymentDate().isBefore(from)) && (to == null || !p.getPaymentDate().isAfter(to))) {
                result.add(toDTO(p, after));
            }
            after = after.add(p.getKind().isRepayment() ? p.getPrincipalAmount() : p.getPrincipalAmount().negate());
        }
        return result;
    }

    // ---------------------------------------------------------------- podpowiedź

    /**
     * Podpowiedź kwot na dany dzień. Bez daty: rata na najbliższy termin po ostatniej racie
     * (albo od dziś), nadpłata na dziś.
     */
    @Transactional(readOnly = true)
    public LoanPaymentSuggestionDTO suggest(Long userId, Long loanId, LoanPaymentKind kind, LocalDate date) {
        Loan loan = ownedLoan(userId, loanId);
        LoanPaymentKind k = kind != null ? kind : LoanPaymentKind.INSTALLMENT;
        LocalDate paymentDate = date != null ? date : defaultDate(loan, k);

        // Kapitał na dzień spłaty: dzisiejszy dług plus to, co spłacono w tym dniu lub później.
        BigDecimal outstanding = outstanding(loan);
        for (LoanPayment p : paymentRepository.findForLoan(loanId, paymentDate, null)) {
            outstanding = outstanding.add(p.getKind().isRepayment() ? p.getPrincipalAmount() : p.getPrincipalAmount().negate());
        }

        List<LoanPayment> earlier = paymentRepository.findRepaymentsBefore(loanId, paymentDate);
        LocalDate interestFrom = earlier.isEmpty() ? loan.lastDueDateBefore(paymentDate) : earlier.get(0).getPaymentDate();
        BigDecimal subsidizedRate = loan.hasSubsidyOn(paymentDate) ? loan.getSubsidizedRate() : null;

        LoanCalculator.Suggestion s = LoanCalculator.suggest(new LoanCalculator.Input(
                k, paymentDate, outstanding.max(BigDecimal.ZERO), interestFrom,
                loan.getAnnualInterestRate(), subsidizedRate, loan.getInstallmentType(),
                loan.plannedEndDate(), currentInstallmentPrincipal(earlier)));

        Account payer = loan.getDefaultPaymentAccount();
        return new LoanPaymentSuggestionDTO(k, paymentDate, s.principal(), s.interest(), s.subsidy(), s.fee(),
                s.payerAmount(), s.interestFrom(), s.interestDays(), s.remainingInstallments(),
                outstanding, payer != null ? payer.getId() : null,
                k == LoanPaymentKind.OVERPAYMENT ? loan.getOverpaymentStrategy() : null);
    }

    /**
     * Część kapitałowa ostatniej raty, jeśli nadal obowiązuje: przy racie malejącej jest stała,
     * dopóki nadpłata ze strategią „niższa rata” nie przeliczy jej od nowa (wtedy null).
     */
    public static BigDecimal currentInstallmentPrincipal(List<LoanPayment> newestFirst) {
        for (LoanPayment p : newestFirst) {
            if (p.getKind() == LoanPaymentKind.OVERPAYMENT && p.getStrategy() == OverpaymentStrategy.REDUCE_INSTALLMENT) {
                return null;
            }
            if (p.getKind() == LoanPaymentKind.INSTALLMENT && p.getPrincipalAmount().signum() > 0) {
                return p.getPrincipalAmount();
            }
        }
        return null;
    }

    private LocalDate defaultDate(Loan loan, LoanPaymentKind kind) {
        LocalDate today = LocalDate.now();
        if (kind != LoanPaymentKind.INSTALLMENT) {
            return today;
        }
        return paymentRepository.findRepaymentsBefore(loan.getId(), LocalDate.of(9999, 12, 31)).stream()
                .filter(p -> p.getKind() == LoanPaymentKind.INSTALLMENT)
                .findFirst()
                .map(last -> loan.nextPaymentDate(last.getPaymentDate().plusDays(1)))
                .orElse(loan.nextPaymentDate(today));
    }


    // ---------------------------------------------------------------- wyrównanie do wyciągu

    /**
     * Ustawia dług na kwotę z wyciągu. Dług mniejszy niż w aplikacji: korekta na koncie kredytu.
     * Dług większy: wydatek „Prowizje i opłaty” doliczony do kredytu (np. skapitalizowane opłaty).
     */
    @Transactional
    public LoanDTO reconcile(Long userId, Long loanId, ReconcileLoanRequest request) {
        Loan loan = activeLoan(userId, loanId);
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(userId));
        Account account = loan.getAccount();
        BigDecimal target = request.outstanding().setScale(2, RoundingMode.HALF_UP);
        BigDecimal diff = target.subtract(outstanding(loan)); // > 0: dług większy niż w aplikacji
        if (diff.signum() == 0) {
            return loanService.getLoan(userId, loanId);
        }
        LocalDate date = request.date() != null ? request.date() : LocalDate.now();
        String description = blankToNull(request.description()) != null ? request.description().trim()
                : "Wyrównanie do wyciągu: " + target + " " + account.getCurrency();

        Transaction t = new Transaction();
        t.setAmount(diff.abs());
        t.setTransactionDate(date);
        t.setDescription(description);
        t.setAccount(account);
        t.setUser(user);
        if (diff.signum() > 0) {
            t.setType(CategoryType.EXPENSE);
            t.setCategory(systemCategory(SystemCategory.LOAN_COSTS));
            t.setSubcategory(systemSubcategory(SystemCategory.LOAN_FEES));
        } else {
            Category adjustment = systemCategory(SystemCategory.ADJUSTMENT);
            t.setType(CategoryType.ADJUSTMENT);
            t.setCategory(adjustment);
            t.setSubcategory(subcategoryRepository.findBySystemKey(SystemCategory.ADJUSTMENT_MANUAL).orElse(null));
        }
        accountBalanceService.applyDeltas(Map.of(account.getId(), diff.negate()));
        transactionRepository.save(t);
        return loanService.getLoan(userId, loanId);
    }

    // ---------------------------------------------------------------- części spłaty

    /** Kwoty spłaty po walidacji, zaokrąglone do grosza. */
    record Amounts(BigDecimal principal, BigDecimal interest, BigDecimal subsidy, BigDecimal fee) {

        static Amounts of(LoanPayment p) {
            return new Amounts(p.getPrincipalAmount(), p.getInterestAmount(), p.getSubsidyAmount(), p.getFeeAmount());
        }

        static Amounts of(LoanPaymentRequest r, Loan loan) {
            Amounts a = new Amounts(money(r.principalAmount()), money(r.interestAmount()),
                    money(r.subsidyAmount()), money(r.feeAmount()));
            if (a.subsidy.compareTo(a.interest) > 0) {
                throw new IllegalArgumentException("Subsidy cannot exceed the interest");
            }
            switch (r.kind()) {
                case INSTALLMENT -> {
                    if (a.principal.add(a.interest).add(a.fee).signum() == 0) {
                        throw new IllegalArgumentException("Installment must have principal, interest or fee");
                    }
                }
                case OVERPAYMENT -> {
                    if (a.principal.signum() == 0) {
                        throw new IllegalArgumentException("Overpayment must have a principal amount");
                    }
                }
                case DISBURSEMENT -> {
                    if (a.principal.signum() == 0 || a.interest.signum() != 0 || a.subsidy.signum() != 0) {
                        throw new IllegalArgumentException("Disbursement has only a principal amount and an optional fee");
                    }
                    if (a.fee.compareTo(a.principal) >= 0) {
                        throw new IllegalArgumentException("Disbursement fee must be lower than the amount");
                    }
                }
            }
            if (r.paymentDate().isBefore(loan.getStartDate())) {
                throw new IllegalArgumentException("Payment date cannot be before the loan start date");
            }
            return a;
        }

        BigDecimal interestPaid() {
            return interest.subtract(subsidy);
        }
    }

    /**
     * Zmiany sald spłaty (sign = 1) albo jej cofnięcia (sign = −1). Spłata: płatnik traci kapitał,
     * odsetki po dopłacie i opłaty, dług maleje o kapitał. Wypłata: płatnik dostaje kapitał minus
     * opłata, dług rośnie o kapitał.
     */
    static void addDeltas(Map<Long, BigDecimal> deltas, Loan loan, Account payer, LoanPaymentKind kind,
                          Amounts a, int sign) {
        BigDecimal s = BigDecimal.valueOf(sign);
        BigDecimal payerDelta;
        BigDecimal loanDelta;
        if (kind.isRepayment()) {
            payerDelta = a.principal().add(a.interestPaid()).add(a.fee()).negate();
            loanDelta = a.principal();
        } else {
            payerDelta = a.principal().subtract(a.fee());
            loanDelta = a.principal().negate();
        }
        deltas.merge(payer.getId(), payerDelta.multiply(s), BigDecimal::add);
        deltas.merge(loan.getAccount().getId(), loanDelta.multiply(s), BigDecimal::add);
    }

    private void fill(LoanPayment payment, LoanPaymentRequest request, Amounts amounts, Account payer, Loan loan) {
        payment.setKind(request.kind());
        payment.setPaymentDate(request.paymentDate());
        payment.setPrincipalAmount(amounts.principal());
        payment.setInterestAmount(amounts.interest());
        payment.setSubsidyAmount(amounts.subsidy());
        payment.setFeeAmount(amounts.fee());
        payment.setStrategy(request.kind() == LoanPaymentKind.OVERPAYMENT
                ? (request.strategy() != null ? request.strategy() : loan.getOverpaymentStrategy())
                : null);
        payment.setPayerAccount(payer);
        payment.setDescription(blankToNull(request.description()));
    }

    /**
     * Tworzy, poprawia albo usuwa przelew i wydatki tak, żeby odpowiadały kwotom spłaty.
     * Opisy mówią, z czego pochodzą (np. „Hipoteka · rata · odsetki”).
     */
    private void syncParts(LoanPayment payment, User user) {
        Loan loan = payment.getLoan();
        Account loanAccount = loan.getAccount();
        Account payer = payment.getPayerAccount();
        String label = loanAccount.getName() + " · " + kindLabel(payment.getKind())
                + (payment.getDescription() != null ? " · " + payment.getDescription() : "");

        // Kapitał: przelew płatnik → kredyt (spłata) albo kredyt → płatnik (wypłata).
        Transfer transfer = payment.getTransfer();
        if (payment.getPrincipalAmount().signum() > 0) {
            if (transfer == null) {
                transfer = new Transfer();
                transfer.setUser(user);
            }
            boolean repayment = payment.getKind().isRepayment();
            transfer.setFromAccount(repayment ? payer : loanAccount);
            transfer.setToAccount(repayment ? loanAccount : payer);
            transfer.setAmount(payment.getPrincipalAmount());
            transfer.setReceivedAmount(payment.getPrincipalAmount());
            transfer.setExchangeRate(BigDecimal.ONE);
            transfer.setTransferDate(payment.getPaymentDate());
            transfer.setDescription(truncate(label + (repayment ? " · kapitał" : "")));
            payment.setTransfer(transferRepository.save(transfer));
        } else if (transfer != null) {
            payment.setTransfer(null);
            paymentRepository.saveAndFlush(payment);
            transferRepository.delete(transfer);
        }

        BigDecimal interestPaid = payment.interestPaid();
        String interestLabel = label + " · odsetki" + (payment.getSubsidyAmount().signum() > 0
                ? " (dopłata " + payment.getSubsidyAmount() + ")" : "");
        payment.setInterestTransaction(syncExpense(payment, payment.getInterestTransaction(), interestPaid,
                SystemCategory.LOAN_INTEREST, interestLabel, user, true));
        payment.setFeeTransaction(syncExpense(payment, payment.getFeeTransaction(), payment.getFeeAmount(),
                SystemCategory.LOAN_FEES, label + " · opłaty", user, false));
    }

    private Transaction syncExpense(LoanPayment payment, Transaction existing, BigDecimal amount,
                                    String subcategoryKey, String description, User user, boolean interest) {
        if (amount.signum() > 0) {
            Transaction t = existing != null ? existing : new Transaction();
            if (existing == null) {
                t.setUser(user);
                t.setType(CategoryType.EXPENSE);
            }
            t.setAmount(amount);
            t.setTransactionDate(payment.getPaymentDate());
            t.setAccount(payment.getPayerAccount());
            t.setCategory(systemCategory(SystemCategory.LOAN_COSTS));
            t.setSubcategory(systemSubcategory(subcategoryKey));
            t.setDescription(truncate(description));
            return transactionRepository.save(t);
        }
        if (existing != null) {
            if (interest) payment.setInterestTransaction(null); else payment.setFeeTransaction(null);
            paymentRepository.saveAndFlush(payment);
            transactionRepository.delete(existing);
        }
        return null;
    }

    // ---------------------------------------------------------------- pomocnicze

    private Loan ownedLoan(Long userId, Long loanId) {
        return loanRepository.findByIdForUser(loanId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));
    }

    private Loan activeLoan(Long userId, Long loanId) {
        Loan loan = ownedLoan(userId, loanId);
        if (loan.getAccount().getDeletedAt() != null) {
            throw new IllegalStateException("Loan account is archived");
        }
        return loan;
    }

    private LoanPayment payment(Loan loan, Long paymentId) {
        return paymentRepository.findByIdAndLoanId(paymentId, loan.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan payment", paymentId));
    }

    /** Konto płatnika: dostępne dla użytkownika, aktywne, nie kredyt, w walucie kredytu. */
    private Account payerAccount(Long accountId, Long userId, Loan loan) {
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
        String currency = loan.getAccount().getCurrency();
        if (!account.getCurrency().equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("Payment account must use the loan currency (" + currency + ")");
        }
        return account;
    }

    private Category systemCategory(String key) {
        return categoryRepository.findBySystemKey(key)
                .orElseThrow(() -> new IllegalStateException("System category " + key + " is missing (migration V15)"));
    }

    private Subcategory systemSubcategory(String key) {
        return subcategoryRepository.findBySystemKey(key)
                .orElseThrow(() -> new IllegalStateException("System subcategory " + key + " is missing (migration V15)"));
    }

    private static BigDecimal outstanding(Loan loan) {
        BigDecimal balance = loan.getAccount().getBalance();
        return balance == null ? BigDecimal.ZERO : balance.negate();
    }

    static LoanPaymentDTO toDTO(LoanPayment p, BigDecimal outstandingAfter) {
        Account payer = p.getPayerAccount();
        return new LoanPaymentDTO(p.getId(), p.getLoan().getId(), p.getKind(), p.getPaymentDate(),
                p.getPrincipalAmount(), p.getInterestAmount(), p.getSubsidyAmount(), p.interestPaid(),
                p.getFeeAmount(), p.payerAmount(), p.getStrategy(),
                payer.getId(), payer.getName(),
                p.getTransfer() != null ? p.getTransfer().getId() : null,
                p.getInterestTransaction() != null ? p.getInterestTransaction().getId() : null,
                p.getFeeTransaction() != null ? p.getFeeTransaction().getId() : null,
                p.getDescription(), outstandingAfter);
    }

    private static String kindLabel(LoanPaymentKind kind) {
        return switch (kind) {
            case INSTALLMENT -> "rata";
            case OVERPAYMENT -> "nadpłata";
            case DISBURSEMENT -> "wypłata";
        };
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String truncate(String value) {
        return value.length() <= 255 ? value : value.substring(0, 255);
    }
}