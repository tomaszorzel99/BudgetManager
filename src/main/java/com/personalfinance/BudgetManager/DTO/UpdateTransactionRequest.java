package com.personalfinance.BudgetManager.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateTransactionRequest {

    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 18, fraction = 2, message = "Amount can have at most 2 decimal places")
    private BigDecimal amount;
    @Size(max = 255, message = "Description is too long")
    private String description;
    private LocalDate transactionDate;
    private Long categoryId;
    private Long subcategoryId;
    /** true = usuń podkategorię z transakcji. Nie łączyć z subcategoryId. */
    private Boolean clearSubcategory;
}