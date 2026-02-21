package com.personalfinance.BudgetManager.DTO;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ComprehensiveReportDTO {

    private int month;
    private int year;

    private MonthlyReportDTO monthly;
    private CumulativeReportDTO cumulative;
}
