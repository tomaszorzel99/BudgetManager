package com.personalfinance.BudgetManager.DTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MonthlyReportDTO {

    private String monthName;
    private int year;
    private BigDecimal income;
    private BigDecimal expenses;
    private BigDecimal balance;

    public MonthlyReportDTO(String monthName, int year, BigDecimal income, BigDecimal expenses, BigDecimal balance) {
        this.monthName = monthName;
        this.year = year;
        this.income = income;
        this.expenses = expenses;
        this.balance = balance;
    }
}
