package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.DTO.ComprehensiveReportDTO;
import com.personalfinance.BudgetManager.DTO.CumulativeReportDTO;
import com.personalfinance.BudgetManager.DTO.MonthlyReportDTO;
import com.personalfinance.BudgetManager.DTO.MonthlyReportRequest;
import com.personalfinance.BudgetManager.Services.ReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyReportDTO> getMonthlyReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam int month,
            @RequestParam int year) {

        MonthlyReportDTO report = reportService.getMonthlyReport(userDetails.getUsername(), month, year);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/cumulative")
    public ResponseEntity<CumulativeReportDTO> getCumulativeReport(
            @RequestParam int month,
            @RequestParam int year,
            @AuthenticationPrincipal UserDetails userDetails) {

        CumulativeReportDTO report = reportService.getCumulativeReport(
                userDetails.getUsername(), month, year);

        return ResponseEntity.ok(report);
    }

    @GetMapping("/comprehensive")
    public ResponseEntity<ComprehensiveReportDTO> getComprehensiveReport(
            @RequestParam int month,
            @RequestParam int year,
            @AuthenticationPrincipal UserDetails userDetails) {

        ComprehensiveReportDTO report = reportService.getComprehensiveReport(
                userDetails.getUsername(), month, year);

        return ResponseEntity.ok(report);
    }
}
