package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.Exception.CategoryException;
import com.personalfinance.BudgetManager.Model.CategoryType;
import com.personalfinance.BudgetManager.Model.Transaction;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class TransactionSpecification {

    public static Specification<Transaction> belongsToGroup(Long groupId) {
        return (root, query, cb) -> {
            assert query != null;
            query.distinct(true);
            Join<Object, Object> userJoin = root.join("user");
            Join<Object, Object> groupJoin = userJoin.join("userGroups");

            return cb.equal(groupJoin.get("id"), groupId);
        };
    }
    public static Specification<Transaction> hasUser(Long userId) {
        return (root, query, cb) ->
                cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Transaction> hasType(CategoryType type) {
        return (root, query, cb) ->
                cb.equal(root.get("type"), type);
    }

    public static Specification<Transaction> hasCategory(Long categoryId) {
        return (root, query, cb) ->
                cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Transaction> inMonth(int month, int year) {
        LocalDate start = LocalDate.of(month, year, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        return (root, query, cb) ->
            cb.between(root.get("transactionDate"), start, end);
    }
}
