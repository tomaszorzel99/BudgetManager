package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.BudgetDTO;
import com.personalfinance.BudgetManager.DTO.CreateBudgetRequest;
import com.personalfinance.BudgetManager.DTO.UpdateBudgetRequest;
import com.personalfinance.BudgetManager.Exception.CategoryException;
import com.personalfinance.BudgetManager.Exception.ResourceNotFoundException;
import com.personalfinance.BudgetManager.Exception.UserException;
import com.personalfinance.BudgetManager.Model.Budget;
import com.personalfinance.BudgetManager.Model.Category;
import com.personalfinance.BudgetManager.Model.CategoryType;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Repositories.BudgetRepository;
import com.personalfinance.BudgetManager.Repositories.CategoryRepository;
import com.personalfinance.BudgetManager.Repositories.TransactionRepository;
import com.personalfinance.BudgetManager.Repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class BudgetService {

    static final BigDecimal WARNING_THRESHOLD = new BigDecimal("80");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public BudgetService(BudgetRepository budgetRepository, CategoryRepository categoryRepository,
                         TransactionRepository transactionRepository, UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<BudgetDTO> getBudgets(Long userId, Integer month, Integer year) {
        YearMonth period = period(month, year);
        LocalDate start = period.atDay(1);
        LocalDate end = period.atEndOfMonth();

        List<Budget> budgets = budgetRepository.findAllForUser(userId);
        Map<String, Map<Long, BigDecimal>> spentByCurrency = new HashMap<>();
        List<BudgetDTO> result = new ArrayList<>();
        for (Budget budget : budgets) {
            Map<Long, BigDecimal> spent = spentByCurrency.computeIfAbsent(budget.getCurrency(),
                    currency -> spentByCategory(userId, currency, start, end));
            result.add(toDTO(budget, spent.getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO), period));
        }
        result.sort(Comparator.comparing(BudgetDTO::percent).reversed());
        return result;
    }

    @Transactional
    public BudgetDTO createBudget(Long userId, CreateBudgetRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(userId));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CategoryException(request.categoryId()));
        // Kategoria cudzej grupy jest dla użytkownika niewidoczna, więc „nie istnieje” (404).
        if (!category.isSystem()
                && (category.getGroup() == null || !UserGroupService.isMember(user, category.getGroup()))) {
            throw new CategoryException(request.categoryId());
        }
        if (category.getType() != CategoryType.EXPENSE) {
            throw new IllegalArgumentException("Budgets can only be set for expense categories");
        }
        String currency = normalizeCurrency(request.currency());
        if (budgetRepository.existsByUserIdAndCategoryIdAndCurrency(userId, category.getId(), currency)) {
            throw new IllegalStateException("Budget for this category already exists");
        }
        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        budget.setCurrency(currency);
        budgetRepository.save(budget);
        return statusNow(userId, budget);
    }

    @Transactional
    public BudgetDTO updateBudget(Long userId, Long id, UpdateBudgetRequest request) {
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));
        budget.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        return statusNow(userId, budget);
    }

    @Transactional
    public void deleteBudget(Long userId, Long id) {
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));
        budgetRepository.delete(budget);
    }

    private BudgetDTO statusNow(Long userId, Budget budget) {
        YearMonth now = YearMonth.now();
        BigDecimal spent = spentByCategory(userId, budget.getCurrency(), now.atDay(1), now.atEndOfMonth())
                .getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO);
        return toDTO(budget, spent, now);
    }

    /**
     * Wydane w kategoriach: wszystkie wydatki na kontach grup użytkownika (także partnera ze wspólnego
     * konta), bo kategoria należy do grupy, a budżet pilnuje wydatków domu, nie tylko własnych.
     */
    private Map<Long, BigDecimal> spentByCategory(Long userId, String currency, LocalDate start, LocalDate end) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(userId));
        Map<Long, BigDecimal> spent = new HashMap<>();
        for (Object[] row : transactionRepository.sumExpensesByCategory(CategoryService.groupIds(user), currency, start, end)) {
            spent.put((Long) row[0], (BigDecimal) row[1]);
        }
        return spent;
    }


    static BudgetDTO toDTO(Budget budget, BigDecimal spent, YearMonth period) {
        BigDecimal amount = budget.getAmount();
        BigDecimal percent = amount.signum() == 0 ? BigDecimal.ZERO
                : spent.multiply(HUNDRED).divide(amount, 1, RoundingMode.HALF_UP);
        // status liczony bez zaokrąglenia (79,99% to jeszcze OK)
        BigDecimal scaledSpent = spent.multiply(HUNDRED);
        String status = amount.signum() == 0 ? (spent.signum() > 0 ? "EXCEEDED" : "OK")
                : scaledSpent.compareTo(amount.multiply(HUNDRED)) > 0 ? "EXCEEDED"
                : scaledSpent.compareTo(amount.multiply(WARNING_THRESHOLD)) >= 0 ? "WARNING" : "OK";
        Category category = budget.getCategory();
        return new BudgetDTO(budget.getId(), category.getId(), category.getName(), category.getColor(),
                amount, budget.getCurrency(), period.getMonthValue(), period.getYear(),
                spent, amount.subtract(spent), percent, status);
    }

    private static String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank() ? "PLN" : currency.trim().toUpperCase(Locale.ROOT);
    }

    private static YearMonth period(Integer month, Integer year) {
        if (month == null || year == null) {
            return YearMonth.now();
        }
        if (month < 1 || month > 12 || year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Invalid month or year");
        }
        return YearMonth.of(year, month);
    }
}
