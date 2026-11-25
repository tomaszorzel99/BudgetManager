package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.Exception.UserException;
import com.personalfinance.BudgetManager.Model.*;
import com.personalfinance.BudgetManager.Repositories.TransactionRepository;
import com.personalfinance.BudgetManager.Repositories.TransferRepository;
import com.personalfinance.BudgetManager.Repositories.UserRepository;
import com.personalfinance.BudgetManager.Services.SimpleXlsxWriter.Column;
import com.personalfinance.BudgetManager.Services.SimpleXlsxWriter.ColumnType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.*;

/** Eksport transakcji (CSV) oraz transakcji, przelewów i podsumowania miesięcznego (Excel). */
@Service
public class ExportService {

    private static final Map<CategoryType, String> TYPE_PL = Map.of(
            CategoryType.INCOME, "Przychód",
            CategoryType.EXPENSE, "Wydatek",
            CategoryType.ADJUSTMENT, "Korekta",
            CategoryType.INITIAL_BALANCE, "Saldo początkowe",
            CategoryType.LIABILITY_OPENING, "Saldo początkowe długu");

    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;
    private final UserRepository userRepository;

    public ExportService(TransactionRepository transactionRepository, TransferRepository transferRepository,
                         UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.transferRepository = transferRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public byte[] transactionsCsv(Long userId, LocalDate from, LocalDate to, Long groupId) {
        List<Transaction> transactions = findTransactions(userId, from, to, groupId);
        DecimalFormat money = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.of("pl", "PL")));
        StringBuilder sb = new StringBuilder("﻿"); // BOM: Excel poprawnie odczyta polskie znaki
        sb.append("Data;Typ;Kategoria;Podkategoria;Opis;Konto;Waluta;Kwota;Użytkownik\r\n");
        for (Transaction t : transactions) {
            sb.append(t.getTransactionDate()).append(';')
                    .append(csv(TYPE_PL.getOrDefault(t.getType(), t.getType().name()))).append(';')
                    .append(csv(t.getCategory().getName())).append(';')
                    .append(csv(t.getSubcategory().getName())).append(';')
                    .append(csv(t.getDescription())).append(';')
                    .append(csv(t.getAccount().getName())).append(';')
                    .append(csv(upper(t.getAccount().getCurrency()))).append(';')
                    .append(money.format(signed(t))).append(';')
                    .append(csv(t.getUser().getName())).append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] workbook(Long userId, LocalDate from, LocalDate to, Long groupId) {
        List<Transaction> transactions = findTransactions(userId, from, to, groupId);

        List<Object[]> txRows = new ArrayList<>();
        // klucz: yyyy-MM|waluta -> [przychody, wydatki]
        Map<String, BigDecimal[]> monthly = new TreeMap<>(Comparator.reverseOrder());
        for (Transaction t : transactions) {
            String currency = upper(t.getAccount().getCurrency());
            txRows.add(new Object[]{t.getTransactionDate(), TYPE_PL.getOrDefault(t.getType(), t.getType().name()),
                    t.getCategory().getName(), t.getSubcategory().getName(), t.getDescription(),
                    t.getAccount().getName(), currency, signed(t), t.getUser().getName()});
            if (t.getType() == CategoryType.INCOME || t.getType() == CategoryType.EXPENSE) {
                String key = t.getTransactionDate().toString().substring(0, 7) + "|" + currency;
                BigDecimal[] sums = monthly.computeIfAbsent(key, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                int i = t.getType() == CategoryType.INCOME ? 0 : 1;
                sums[i] = sums[i].add(t.getAmount());
            }
        }

        List<Object[]> summaryRows = new ArrayList<>();
        monthly.forEach((key, sums) -> {
            String[] parts = key.split("\\|", 2);
            BigDecimal balance = sums[0].subtract(sums[1]);
            BigDecimal rate = sums[0].signum() == 0 ? null
                    : balance.multiply(BigDecimal.valueOf(100)).divide(sums[0], 1, java.math.RoundingMode.HALF_UP);
            summaryRows.add(new Object[]{parts[0], parts[1], sums[0], sums[1], balance, rate});
        });

        List<Object[]> trRows = new ArrayList<>();
        for (Transfer t : findTransfers(userId, from, to)) {
            trRows.add(new Object[]{t.getTransferDate(), t.getFromAccount().getName(), t.getToAccount().getName(),
                    t.getAmount(), upper(t.getFromAccount().getCurrency()), t.getReceivedAmount(),
                    upper(t.getToAccount().getCurrency()), t.getExchangeRate(), t.getDescription()});
        }

        return new SimpleXlsxWriter()
                .addSheet("Transakcje", List.of(
                        new Column("Data", 12, ColumnType.DATE), new Column("Typ", 14, ColumnType.TEXT),
                        new Column("Kategoria", 20, ColumnType.TEXT), new Column("Podkategoria", 20, ColumnType.TEXT),
                        new Column("Opis", 34, ColumnType.TEXT), new Column("Konto", 20, ColumnType.TEXT),
                        new Column("Waluta", 8, ColumnType.TEXT), new Column("Kwota", 14, ColumnType.MONEY),
                        new Column("Użytkownik", 16, ColumnType.TEXT)), txRows)
                .addSheet("Podsumowanie", List.of(
                                new Column("Miesiąc", 10, ColumnType.TEXT), new Column("Waluta", 8, ColumnType.TEXT),
                                new Column("Przychody", 14, ColumnType.MONEY), new Column("Wydatki", 14, ColumnType.MONEY),
                                new Column("Bilans", 14, ColumnType.MONEY), new Column("Stopa oszczędności %", 20, ColumnType.NUMBER)),
                        summaryRows)
                .addSheet("Przelewy", List.of(
                        new Column("Data", 12, ColumnType.DATE), new Column("Z konta", 20, ColumnType.TEXT),
                        new Column("Na konto", 20, ColumnType.TEXT), new Column("Kwota", 14, ColumnType.MONEY),
                        new Column("Waluta", 8, ColumnType.TEXT), new Column("Kwota otrzymana", 16, ColumnType.MONEY),
                        new Column("Waluta docelowa", 15, ColumnType.TEXT), new Column("Kurs", 12, ColumnType.NUMBER),
                        new Column("Opis", 30, ColumnType.TEXT)), trRows)
                .toBytes();
    }

    private List<Transaction> findTransactions(Long userId, LocalDate from, LocalDate to, Long groupId) {
        validateRange(from, to);
        Specification<Transaction> spec;
        if (groupId != null) {
            User user = userRepository.findById(userId).orElseThrow(() -> new UserException(userId));
            if (user.getUserGroups().stream().noneMatch(g -> g.getId().equals(groupId))) {
                throw new AccessDeniedException("No access to this group");
            }
            spec = TransactionSpecification.belongsToGroup(groupId);
        } else {
            spec = TransactionSpecification.hasUser(userId);
        }
        if (from != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("transactionDate"), from));
        }
        if (to != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("transactionDate"), to));
        }
        return transactionRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "transactionDate", "id"));
    }

    private List<Transfer> findTransfers(Long userId, LocalDate from, LocalDate to) {
        List<Transfer> transfers = (from != null || to != null)
                ? transferRepository.findByUserAndDateBetween(userId,
                from != null ? from : LocalDate.of(1900, 1, 1), to != null ? to : LocalDate.of(9999, 12, 31))
                : transferRepository.findByUserId(userId);
        return transfers.stream()
                .sorted(Comparator.comparing(Transfer::getTransferDate).thenComparing(Transfer::getId).reversed())
                .toList();
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("Invalid date range");
        }
    }


    private static BigDecimal signed(Transaction t) {
        return t.getType() == CategoryType.EXPENSE ? t.getAmount().negate() : t.getAmount();
    }

    private static String subcategoryName(Transaction t) {
            return t.getSubcategory() != null ? t.getSubcategory().getName() : null;
        }

    private static String upper(String s) {
        return s == null ? "" : s.toUpperCase(Locale.ROOT);
    }

    /** Pole CSV ze średnikiem jako separatorem; blokuje formuły (CSV injection). */
    static String csv(String value) {
        if (value == null) return "";
        String v = value;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        if (v.contains(";") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
