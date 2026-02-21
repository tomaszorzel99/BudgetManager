package com.personalfinance.BudgetManager.DTO;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CumulativeReportDTO {

    private int month;
    private int year;

    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal totalTransfersToSavings;
    private BigDecimal totalTransfersFromSavings;
    private BigDecimal netTransfersToSavings;

    private BigDecimal currentAvailableBalance;
    private BigDecimal currentSavingsBalance;
    private BigDecimal currentInvestmentBalance;
    private BigDecimal currentTotalBalance;

    private BigDecimal theoreticalNetIncome;
}
