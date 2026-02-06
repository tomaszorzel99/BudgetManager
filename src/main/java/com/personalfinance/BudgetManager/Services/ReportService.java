package com.personalfinance.BudgetManager.Services;


import com.personalfinance.BudgetManager.DTO.MonthlyReportDTO;
import com.personalfinance.BudgetManager.Exception.UserException;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Repositories.TransactionRepository;
import com.personalfinance.BudgetManager.Repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public ReportService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public MonthlyReportDTO getMonthlyReport(String userEmail, int month, int year){
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserException(userEmail));
        BigDecimal totalExpensesByMonthAndYear = transactionRepository.getTotalExpensesByMonthAndYear(user.getId(), month, year);
        BigDecimal totalIncomeByMonthAndYear = transactionRepository.getTotalIncomeByMonthAndYear(user.getId(), month, year);
        BigDecimal monthlyBalance = transactionRepository.getMonthlyBalance(user.getId(), month, year);

        if(totalExpensesByMonthAndYear == null){
            totalExpensesByMonthAndYear = BigDecimal.ZERO;
        }

        if(totalIncomeByMonthAndYear == null){
            totalIncomeByMonthAndYear = BigDecimal.ZERO;
        }

        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        return new MonthlyReportDTO(monthName, year, totalIncomeByMonthAndYear, totalExpensesByMonthAndYear, monthlyBalance);
    }


}
