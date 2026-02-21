package com.personalfinance.BudgetManager.DTO;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MonthlyReportDTO {

    private int month;
    private int year;

    private BigDecimal income;
    private BigDecimal expenses;
    private BigDecimal transfersToSavings;
    private BigDecimal transfersFromSavings;
    private BigDecimal netSavingsThisMonth;
    private BigDecimal monthlyBalance;
}
