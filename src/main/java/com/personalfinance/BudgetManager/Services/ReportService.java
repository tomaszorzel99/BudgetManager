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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    public MonthlyReportDTO getMonthlyReport(String userEmail, int month, int year){

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new UserException(userEmail));
        BigDecimal income = transactionRepository.getTotalIncomeByMonth(user.getId(), CategoryType.INCOME, startDate, endDate);
        BigDecimal expenses = transactionRepository.getTotalExpensesByMonth(user.getId(), CategoryType.EXPENSE, startDate, endDate);
        BigDecimal transfersToSavings = transferRepository.getTotalTransfersToTypeByMonth(user.getId(), AccountType.SAVINGS, month, year);
        BigDecimal transfersFromSavings = transferRepository.getTotalTransfersFromTypeByMonth(user.getId(), AccountType.SAVINGS, month, year);
        BigDecimal netSavings = transfersToSavings.subtract(transfersFromSavings);

        // Kredyty: kapitał spłacony to przesunięcie majątku, nie wydatek; koszty (odsetki, opłaty) są już w expenses.
        BigDecimal principal = loanPaymentRepository.sumPrincipal(user.getId(), LoanPaymentKind.INSTALLMENT, startDate, endDate);
        BigDecimal overpayments = loanPaymentRepository.sumPrincipal(user.getId(), LoanPaymentKind.OVERPAYMENT, startDate, endDate);
        BigDecimal disbursements = loanPaymentRepository.sumPrincipal(user.getId(), LoanPaymentKind.DISBURSEMENT, startDate, endDate);
        BigDecimal loanCosts = transactionRepository.getTotalExpensesBySystemCategory(user.getId(), SystemCategory.LOAN_COSTS, startDate, endDate);
        BigDecimal subsidy = loanPaymentRepository.sumSubsidy(user.getId(), startDate, endDate);

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

    public CumulativeReportDTO getCumulativeReport(String userEmail, int month, int year) {
        LocalDate endDate = effectiveDate(checkedMonth(month, year));
        //LocalDate endDate = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);

        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new UserException(userEmail));
        BigDecimal totalIncome = transactionRepository.getCumulativeIncome(user.getId(),CategoryType.INCOME, endDate);
        BigDecimal totalExpenses = transactionRepository.getCumulativeExpenses(user.getId(), CategoryType.EXPENSE, endDate);
        BigDecimal totalTransfersToSavings = transferRepository.getCumulativeTransfersToType(user.getId(), AccountType.SAVINGS, endDate);
        BigDecimal totalTransfersFromSavings = transferRepository.getCumulativeTransfersFromType(user.getId(), AccountType.SAVINGS, endDate);
        BigDecimal currentAvailableBalance = accountRepository.getTotalAvailableBalance(user.getId());
        BigDecimal currentSavingBalance = accountRepository.getTotalBalanceByType(user.getId(), AccountType.SAVINGS);
        BigDecimal currentInvestmentBalance = accountRepository.getTotalBalanceByType(user.getId(), AccountType.INVESTMENT);
        BigDecimal currentTotalBalance = accountRepository.getTotalBalance(user.getId());
        BalanceAtDateDTO historical = historicalBalanceService.calculate(user.getId(), endDate);
        BigDecimal currentLiabilities = accountRepository.getTotalBalanceByType(user.getId(), AccountType.LOAN).negate();
        BigDecimal currentProperty = accountRepository.getTotalBalanceByType(user.getId(), AccountType.PROPERTY);
        LocalDate since = LocalDate.of(1, 1, 1);
        BigDecimal totalPrincipal = loanPaymentRepository.sumPrincipal(user.getId(), LoanPaymentKind.INSTALLMENT, since, endDate);
        BigDecimal totalOverpayments = loanPaymentRepository.sumPrincipal(user.getId(), LoanPaymentKind.OVERPAYMENT, since, endDate);
        BigDecimal totalLoanCosts = transactionRepository.getTotalExpensesBySystemCategory(user.getId(), SystemCategory.LOAN_COSTS, since, endDate);
        BigDecimal totalSubsidy = loanPaymentRepository.sumSubsidy(user.getId(), since, endDate);

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

    public ComprehensiveReportDTO getComprehensiveReport(String userEmail, int month, int year) {
        return ComprehensiveReportDTO.builder()
                .month(month)
                .year(year)
                .monthly(getMonthlyReport(userEmail, month, year))
                .cumulative(getCumulativeReport(userEmail, month, year))
                .build();
    }

        public List<BalanceAtDateDTO> getBalanceHistory(
                String userEmail, int month, int year, int months) {

            YearMonth lastMonth = checkedMonth(month, year);

            if (months < 1 || months > 36) {
                throw new IllegalArgumentException(
                        "months must be between 1 and 36");
            }

            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new UserException(userEmail));

            List<BalanceAtDateDTO> points = new ArrayList<>();

            for (int i = months - 1; i >= 0; i--) {
                YearMonth period = lastMonth.minusMonths(i);

                points.add(historicalBalanceService.calculate(
                        user.getId(), effectiveDate(period)));
            }

            return points;
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
