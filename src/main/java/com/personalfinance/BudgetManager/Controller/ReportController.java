package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.DTO.MonthlyReportDTO;
import com.personalfinance.BudgetManager.Services.ReportService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyReportDTO> getMonthlyReport(
            @RequestParam @Email(message = "Email must be valid") String userEmail,
            @RequestParam @Min(1) @Max(12) int month,
            @RequestParam @Min(2000) @Max(2100) int year){
        MonthlyReportDTO monthlyReport = reportService.getMonthlyReport(userEmail, month, year);
        return ResponseEntity.ok(monthlyReport);
    }
}
