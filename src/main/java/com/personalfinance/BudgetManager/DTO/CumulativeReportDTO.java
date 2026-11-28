package com.personalfinance.BudgetManager.DTO;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    private LocalDate balanceDate;
    private String balanceCurrency;
    private BigDecimal totalBalanceAtDate;
    private BigDecimal savingsBalanceAtDate;
    private BigDecimal investmentBalanceAtDate;
    private BigDecimal currentAssets;
    private BigDecimal currentLiabilities;
    private BigDecimal currentNetWorth;
    private BigDecimal liabilitiesAtDate;
    private BigDecimal totalLoanPrincipalRepaid;
    private BigDecimal totalLoanOverpayments;
    private BigDecimal totalLoanCosts;
    private BigDecimal totalLoanSubsidy;
    private BigDecimal currentPropertyValue;
    private BigDecimal currentLiquidNetWorth;
    private BigDecimal propertyAtDate;
}
