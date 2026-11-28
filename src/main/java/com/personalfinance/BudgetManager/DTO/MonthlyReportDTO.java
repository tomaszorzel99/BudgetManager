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
    private BigDecimal loanPrincipalRepaid;
    private BigDecimal loanOverpayments;
    private BigDecimal loanDisbursements;
    private BigDecimal loanCosts;
    private BigDecimal loanSubsidy;
    private BigDecimal totalOutflow;
    private BigDecimal wealthBuilt;
}
