package com.personalfinance.BudgetManager.DTO;


import com.personalfinance.BudgetManager.Model.CategoryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateTransactionRequest {

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 18, fraction = 2, message = "Amount can have at most 2 decimal places")
    private BigDecimal amount;

    @NotNull
    private CategoryType type;

    private LocalDate transactionDate;
    @Size(max = 255, message = "Description is too long")
    private String description;

    @NotNull(message = "Account id cannot be null")
    private Long accountId;

    @NotNull(message = "Category id cannot be null")
    private Long categoryId;

    /** Opcjonalna. Brak = transakcja bez podkategorii („Bez podkategorii” we froncie). */
    private Long subcategoryId;
}