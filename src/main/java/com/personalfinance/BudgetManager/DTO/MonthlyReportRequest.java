package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class MonthlyReportRequest {

    private int month;
    private int year;
}
