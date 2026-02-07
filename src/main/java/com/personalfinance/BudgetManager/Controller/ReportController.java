package com.personalfinance.BudgetManager.Controller;

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
    public ResponseEntity<MonthlyReportDTO> getMonthlyReport(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody MonthlyReportRequest request){
        String userEmail = userDetails.getUsername();
        int month = request.getMonth();
        int year = request.getYear();
        MonthlyReportDTO monthlyReport = reportService.getMonthlyReport(userEmail, month, year);
        return ResponseEntity.ok(monthlyReport);
    }
}
