package com.personalfinance.BudgetManager.Services;


import com.personalfinance.BudgetManager.DTO.BalanceAtDateDTO;
import com.personalfinance.BudgetManager.DTO.ComprehensiveReportDTO;
import com.personalfinance.BudgetManager.DTO.CumulativeReportDTO;
import com.personalfinance.BudgetManager.DTO.MonthlyReportDTO;
import com.personalfinance.BudgetManager.Exception.UserException;
import com.personalfinance.BudgetManager.Model.AccountType;
import com.personalfinance.BudgetManager.Model.CategoryType;
import com.personalfinance.BudgetManager.Model.LoanPaymentKind;
import com.personalfinance.BudgetManager.Model.SystemCategory;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Repositories.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final HistoricalBalanceService historicalBalanceService;
    private final LoanPaymentRepository loanPaymentRepository;

    public ReportService(TransactionRepository transactionRepository, UserRepository userRepository, AccountRepository accountRepository, TransferRepository transferRepository, HistoricalBalanceService historicalBalanceService, LoanPaymentRepository loanPaymentRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.historicalBalanceService = historicalBalanceService;
        this.loanPaymentRepository = loanPaymentRepository;
    }

    @Transactional(readOnly = true)
    public MonthlyReportDTO getMonthlyReport(String userEmail, int month, int year, Long groupId){

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        List<Long> groups = groupIds(userEmail, groupId);
        BigDecimal income = transactionRepository.sumByTypeBetween(groups, CategoryType.INCOME, startDate, endDate);
        BigDecimal expenses = transactionRepository.sumByTypeBetween(groups, CategoryType.EXPENSE, startDate, endDate);
        BigDecimal transfersToSavings = transferRepository.getTotalTransfersToTypeByMonth(groups, AccountType.SAVINGS, month, year);
        BigDecimal transfersFromSavings = transferRepository.getTotalTransfersFromTypeByMonth(groups, AccountType.SAVINGS, month, year);
        BigDecimal netSavings = transfersToSavings.subtract(transfersFromSavings);

        BigDecimal principal = loanPaymentRepository.sumPrincipal(groups, LoanPaymentKind.INSTALLMENT, startDate, endDate);
        BigDecimal overpayments = loanPaymentRepository.sumPrincipal(groups, LoanPaymentKind.OVERPAYMENT, startDate, endDate);
        BigDecimal disbursements = loanPaymentRepository.sumPrincipal(groups, LoanPaymentKind.DISBURSEMENT, startDate, endDate);
        BigDecimal loanCosts = transactionRepository.getTotalExpensesBySystemCategory(groups, SystemCategory.LOAN_COSTS, startDate, endDate);
        BigDecimal subsidy = loanPaymentRepository.sumSubsidy(groups, startDate, endDate);

        return MonthlyReportDTO.builder()
                .month(month)
                .year(year)
                .income(income)
                .expenses(expenses)
                .transfersToSavings(transfersToSavings)
                .transfersFromSavings(transfersFromSavings)
                .netSavingsThisMonth(netSavings)
                .monthlyBalance(income.subtract(expenses))
                .loanPrincipalRepaid(principal)
                .loanOverpayments(overpayments)
                .loanDisbursements(disbursements)
                .loanCosts(loanCosts)
                .loanSubsidy(subsidy)
                .totalOutflow(expenses.add(principal).add(overpayments))
                .wealthBuilt(netSavings.add(principal).add(overpayments).subtract(disbursements))
                .build();
    }

    /** Raport narastający dla kont grup użytkownika (bez groupId: wszystkich jego grup). */
    @Transactional(readOnly = true)
    public CumulativeReportDTO getCumulativeReport(String userEmail, int month, int year, Long groupId) {
        LocalDate endDate = effectiveDate(checkedMonth(month, year));

        List<Long> groups = groupIds(userEmail, groupId);
        BigDecimal totalIncome = transactionRepository.sumByTypeUntil(groups, CategoryType.INCOME, endDate);
        BigDecimal totalExpenses = transactionRepository.sumByTypeUntil(groups, CategoryType.EXPENSE, endDate);
        BigDecimal totalTransfersToSavings = transferRepository.getCumulativeTransfersToType(groups, AccountType.SAVINGS, endDate);
        BigDecimal totalTransfersFromSavings = transferRepository.getCumulativeTransfersFromType(groups, AccountType.SAVINGS, endDate);
        BigDecimal currentAvailableBalance = accountRepository.getTotalAvailableBalance(groups);
        BigDecimal currentSavingBalance = accountRepository.getTotalBalanceByType(groups, AccountType.SAVINGS);
        BigDecimal currentInvestmentBalance = accountRepository.getTotalBalanceByType(groups, AccountType.INVESTMENT);
        BigDecimal currentTotalBalance = accountRepository.getTotalBalance(groups);
        BalanceAtDateDTO historical = historicalBalanceService.calculate(groups, endDate);
        BigDecimal currentLiabilities = accountRepository.getTotalBalanceByType(groups, AccountType.LOAN).negate();
        BigDecimal currentProperty = accountRepository.getTotalBalanceByType(groups, AccountType.PROPERTY);
        LocalDate since = LocalDate.of(1, 1, 1);
        BigDecimal totalPrincipal = loanPaymentRepository.sumPrincipal(groups, LoanPaymentKind.INSTALLMENT, since, endDate);
        BigDecimal totalOverpayments = loanPaymentRepository.sumPrincipal(groups, LoanPaymentKind.OVERPAYMENT, since, endDate);
        BigDecimal totalLoanCosts = transactionRepository.getTotalExpensesBySystemCategory(groups, SystemCategory.LOAN_COSTS, since, endDate);
        BigDecimal totalSubsidy = loanPaymentRepository.sumSubsidy(groups, since, endDate);

        return CumulativeReportDTO.builder()
                .month(month)
                .year(year)
                .totalIncome(totalIncome)
                .totalExpense(totalExpenses)
                .totalTransfersToSavings(totalTransfersToSavings)
                .totalTransfersFromSavings(totalTransfersFromSavings)
                .netTransfersToSavings(totalTransfersToSavings.subtract(totalTransfersFromSavings))
                .currentAvailableBalance(currentAvailableBalance)
                .currentSavingsBalance(currentSavingBalance)
                .currentInvestmentBalance(currentInvestmentBalance)
                .currentTotalBalance(currentTotalBalance)
                .theoreticalNetIncome(totalIncome.subtract(totalExpenses))
                .balanceDate(historical.balanceDate())
                .balanceCurrency(historical.currency())
                .totalBalanceAtDate(historical.totalBalance())
                .savingsBalanceAtDate(historical.savingsBalance())
                .investmentBalanceAtDate(historical.investmentBalance())
                .currentAssets(currentTotalBalance.add(currentLiabilities))
                .currentLiabilities(currentLiabilities)
                .currentNetWorth(currentTotalBalance)
                .liabilitiesAtDate(historical.liabilitiesBalance() == null ? BigDecimal.ZERO : historical.liabilitiesBalance().negate())
                .currentPropertyValue(currentProperty)
                .currentLiquidNetWorth(currentTotalBalance.add(currentLiabilities).subtract(currentProperty))
                .propertyAtDate(historical.propertyBalance() == null ? BigDecimal.ZERO : historical.propertyBalance())
                .totalLoanPrincipalRepaid(totalPrincipal.add(totalOverpayments))
                .totalLoanOverpayments(totalOverpayments)
                .totalLoanCosts(totalLoanCosts)
                .totalLoanSubsidy(totalSubsidy)
                .theoreticalNetIncome(
                        totalIncome.subtract(totalExpenses))
                .build();
    }

    @Transactional(readOnly = true)
    public ComprehensiveReportDTO getComprehensiveReport(String userEmail, int month, int year, Long groupId) {
        return ComprehensiveReportDTO.builder()
                .month(month)
                .year(year)
                .monthly(getMonthlyReport(userEmail, month, year, groupId))
                .cumulative(getCumulativeReport(userEmail, month, year, groupId))
                .build();
    }

    @Transactional(readOnly = true)
    public List<BalanceAtDateDTO> getBalanceHistory(String userEmail, int month, int year, int months, Long groupId) {
        YearMonth lastMonth = checkedMonth(month, year);
        if (months < 1 || months > 36) {
            throw new IllegalArgumentException("months must be between 1 and 36");
        }
        List<Long> groups = groupIds(userEmail, groupId);
        List<BalanceAtDateDTO> points = new ArrayList<>();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth period = lastMonth.minusMonths(i);
            points.add(historicalBalanceService.calculate(groups, effectiveDate(period)));
        }
        return points;
    }

    /** Wszystkie grupy użytkownika albo jedna wybrana (wtedy musi do niej należeć). */
    private List<Long> groupIds(String userEmail, Long groupId) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new UserException(userEmail));
        if (groupId == null) {
            return CategoryService.groupIds(user);
        }
        if (user.getUserGroups().stream().noneMatch(g -> g.getId().equals(groupId))) {
            throw new AccessDeniedException("No access to this group");
        }
        return List.of(groupId);
    }

    private YearMonth checkedMonth(int month, int year) {
        if (month < 1 || month > 12 || year < 1 || year > 9999) {
            throw new IllegalArgumentException(
                    "Invalid month or year");
        }

        YearMonth period = YearMonth.of(year, month);

        if (period.isAfter(YearMonth.now())) {
            throw new IllegalArgumentException(
                    "Future months are not available in reports");
        }

        return period;
    }

    private LocalDate effectiveDate(YearMonth period) {
        LocalDate endOfMonth = period.atEndOfMonth();
        LocalDate today = LocalDate.now();

        return endOfMonth.isAfter(today) ? today : endOfMonth;
    }
}
