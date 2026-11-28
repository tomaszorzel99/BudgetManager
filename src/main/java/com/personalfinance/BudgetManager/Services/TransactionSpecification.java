package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.Exception.CategoryException;
import com.personalfinance.BudgetManager.Model.CategoryType;
import com.personalfinance.BudgetManager.Model.Subcategory;
import com.personalfinance.BudgetManager.Model.Transaction;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Locale;

public class TransactionSpecification {

    public static Specification<Transaction> belongsToGroup(Long groupId) {
        return (root, query, cb) ->
                cb.equal(root.get("account").get("group").get("id"), groupId);
    }
    /** Transakcje na kontach podanych grup (lista i raporty grupy, a nie tylko wpisy autora). */
    public static Specification<Transaction> inGroups(Collection<Long> groupIds) {
        return (root, query, cb) -> root.get("account").get("group").get("id").in(groupIds);
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

    public static Specification<Transaction> hasAccount(Long accountId) {
        return (root, query, cb) ->
                cb.equal(root.get("account").get("id"), accountId);
    }

    public static Specification<Transaction> inMonth(int month, int year) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        return (root, query, cb) ->
            cb.between(root.get("transactionDate"), start, end);
    }

    public static Specification<Transaction> from(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("transactionDate"), from);
    }

    public static Specification<Transaction> to(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("transactionDate"), to);
    }

    /** Szukanie w opisie, nazwie kategorii i podkategorii (bez rozróżniania wielkości liter). */
    public static Specification<Transaction> matches(String text) {
        String pattern = "%" + text.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> {
            Join<Transaction, Subcategory> subcategory = root.join("subcategory", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(root.get("description")), pattern, '\\'),
                    cb.like(cb.lower(root.get("category").get("name")), pattern, '\\'),
                    cb.like(cb.lower(subcategory.get("name")), pattern, '\\'));
        };
    }
}
