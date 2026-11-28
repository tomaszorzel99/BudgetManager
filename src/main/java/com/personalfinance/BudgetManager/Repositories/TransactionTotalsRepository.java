package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.DTO.CurrencyTotalsDTO;
import com.personalfinance.BudgetManager.Model.Transaction;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface TransactionTotalsRepository {

    List<CurrencyTotalsDTO> totalsByCurrency(Specification<Transaction> spec);
}