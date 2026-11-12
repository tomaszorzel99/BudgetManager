package com.personalfinance.BudgetManager.Services;


import com.personalfinance.BudgetManager.DTO.BalanceAtDateDTO;
import com.personalfinance.BudgetManager.DTO.ComprehensiveReportDTO;
import com.personalfinance.BudgetManager.DTO.CumulativeReportDTO;
import com.personalfinance.BudgetManager.DTO.MonthlyReportDTO;
import com.personalfinance.BudgetManager.Exception.UserException;
import com.personalfinance.BudgetManager.Model.AccountType;
import com.personalfinance.BudgetManager.Model.CategoryType;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Repositories.AccountRepository;
import com.personalfinance.BudgetManager.Repositories.TransactionRepository;
import com.personalfinance.BudgetManager.Repositories.TransferRepository;
import com.personalfinance.BudgetManager.Repositories.UserRepository;
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

    public ReportService(TransactionRepository transactionRepository, UserRepository userRepository, AccountRepository accountRepository, TransferRepository transferRepository, HistoricalBalanceService historicalBalanceService) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.historicalBalanceService = historicalBalanceService;
    }

    public MonthlyReportDTO getMonthlyReport(String userEmail, int month, int year){

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new UserException(userEmail));
        BigDecimal income = transactionRepository.getTotalIncomeByMonth(user.getId(), CategoryType.INCOME, startDate, endDate);
        BigDecimal expenses = transactionRepository.getTotalExpensesByMonth(user.getId(), CategoryType.EXPENSE, startDate, endDate);
        BigDecimal transfersToSavings = transferRepository.getTotalTransfersToTypeByMonth(user.getId(), AccountType.SAVINGS, month, year);
        BigDecimal transfersFromSavings = transferRepository.getTotalTransfersFromTypeByMonth(user.getId(), AccountType.SAVINGS, month, year);

        return MonthlyReportDTO.builder()
                .month(month)
                .year(year)
                .income(income)
                .expenses(expenses)
                .transfersToSavings(transfersToSavings)
                .transfersFromSavings(transfersFromSavings)
                .netSavingsThisMonth(transfersToSavings.subtract(transfersFromSavings))
                .monthlyBalance(income.subtract(expenses))
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
