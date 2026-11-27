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

    // Kredyty (V15)
    /** Aktywa dziś: suma kont bez kredytów. */
    private BigDecimal currentAssets;
    /** Zobowiązania dziś: dług na kontach kredytów (liczba dodatnia). */
    private BigDecimal currentLiabilities;
    /** Wartość netto dziś = aktywa − zobowiązania (to samo co currentTotalBalance). */
    private BigDecimal currentNetWorth;
    /** Zobowiązania na dzień raportu (liczba dodatnia). */
    private BigDecimal liabilitiesAtDate;
    private BigDecimal totalLoanPrincipalRepaid;
    private BigDecimal totalLoanOverpayments;
    private BigDecimal totalLoanCosts;
    private BigDecimal totalLoanSubsidy;

    // Nieruchomości (V16)
    /** Wartość nieruchomości dziś (zawarta w aktywach i w wartości netto). */
    private BigDecimal currentPropertyValue;
    /** Majątek płynny dziś: konta bez kredytów i bez nieruchomości. */
    private BigDecimal currentLiquidNetWorth;
    /** Wartość nieruchomości na dzień raportu. */
    private BigDecimal propertyAtDate;
}
