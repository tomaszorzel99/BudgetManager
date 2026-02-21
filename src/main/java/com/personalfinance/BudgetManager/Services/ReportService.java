package com.personalfinance.BudgetManager.Services;


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
import java.time.format.TextStyle;
import java.util.Locale;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;

    public ReportService(TransactionRepository transactionRepository, UserRepository userRepository, AccountRepository accountRepository, TransferRepository transferRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
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
        LocalDate endDate = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);

        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new UserException(userEmail));
        BigDecimal totalIncome = transactionRepository.getCumulativeIncome(user.getId(),CategoryType.INCOME, endDate);
        BigDecimal totalExpenses = transactionRepository.getCumulativeExpenses(user.getId(), CategoryType.EXPENSE, endDate);
        BigDecimal totalTransfersToSavings = transferRepository.getCumulativeTransfersToType(user.getId(), AccountType.SAVINGS, endDate);
        BigDecimal totalTransfersFromSavings = transferRepository.getCumulativeTransfersFromType(user.getId(), AccountType.SAVINGS, endDate);
        BigDecimal currentAvailableBalance = accountRepository.getTotalAvailableBalance(user.getId());
        BigDecimal currentSavingBalance = accountRepository.getTotalBalanceByType(user.getId(), AccountType.SAVINGS);
        BigDecimal currentInvestmentBalance = accountRepository.getTotalBalanceByType(user.getId(), AccountType.INVESTMENT);
        BigDecimal currentTotalBalance = accountRepository.getTotalBalance(user.getId());

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
}
