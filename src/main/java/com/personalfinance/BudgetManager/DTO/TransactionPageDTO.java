package com.personalfinance.BudgetManager.DTO;

import java.util.List;

public record TransactionPageDTO(List<TransactionDTO> content,
                                 int page,
                                 int size,
                                 long totalElements,
                                 int totalPages,
                                 List<CurrencyTotalsDTO> totals) {
}