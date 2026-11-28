package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.DTO.CurrencyTotalsDTO;
import com.personalfinance.BudgetManager.Model.CategoryType;
import com.personalfinance.BudgetManager.Model.Transaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class TransactionTotalsRepositoryImpl implements TransactionTotalsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<CurrencyTotalsDTO> totalsByCurrency(Specification<Transaction> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Transaction> root = query.from(Transaction.class);
        Expression<String> currency = root.get("account").get("currency");
        Expression<CategoryType> type = root.get("type");
        Expression<BigDecimal> sum = cb.sum(root.<BigDecimal>get("amount"));
        query.multiselect(currency, type, sum);
        Predicate where = spec == null ? null : spec.toPredicate(root, query, cb);
        if (where != null) {
            query.where(where);
        }
        query.groupBy(currency, type);

        Map<String, BigDecimal[]> byCurrency = new TreeMap<>();
        for (Tuple row : entityManager.createQuery(query).getResultList()) {
            String code = row.get(0, String.class).toUpperCase(Locale.ROOT);
            BigDecimal[] totals = byCurrency.computeIfAbsent(code, c -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            CategoryType rowType = row.get(1, CategoryType.class);
            BigDecimal amount = row.get(2, BigDecimal.class);
            if (rowType == CategoryType.INCOME) {
                totals[0] = totals[0].add(amount);
            } else if (rowType == CategoryType.EXPENSE) {
                totals[1] = totals[1].add(amount);
            }
        }
        List<CurrencyTotalsDTO> result = new ArrayList<>();
        byCurrency.forEach((code, totals) -> result.add(new CurrencyTotalsDTO(code, totals[0], totals[1])));
        return result;
    }
}