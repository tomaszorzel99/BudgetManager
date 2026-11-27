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

    // Kredyty (V15). Kapitał to nie wydatek: pokazujemy go obok wydatków.
    /** Kapitał ze zwykłych rat. */
    private BigDecimal loanPrincipalRepaid;
    /** Nadpłaty kapitału. */
    private BigDecimal loanOverpayments;
    /** Wypłaty z kredytu (dług wzrósł). */
    private BigDecimal loanDisbursements;
    /** Odsetki i opłaty od kredytów (kategoria „Kredyty i pożyczki”); już zawarte w {@code expenses}. */
    private BigDecimal loanCosts;
    /** Dopłaty państwa do odsetek; nie są ani wydatkiem, ani przychodem. */
    private BigDecimal loanSubsidy;
    /** Wypływ łącznie: wydatki + kapitał z rat + nadpłaty. */
    private BigDecimal totalOutflow;
    /** Odłożone i spłacone: netto na oszczędności + kapitał + nadpłaty − wypłaty z kredytu. */
    private BigDecimal wealthBuilt;
}
