package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.CreateSavingsGoalRequest;
import com.personalfinance.BudgetManager.DTO.GoalContributionRequest;
import com.personalfinance.BudgetManager.DTO.SavingsGoalDTO;
import com.personalfinance.BudgetManager.DTO.UpdateSavingsGoalRequest;
import com.personalfinance.BudgetManager.Exception.AccountException;
import com.personalfinance.BudgetManager.Exception.ResourceNotFoundException;
import com.personalfinance.BudgetManager.Exception.UserException;
import com.personalfinance.BudgetManager.Model.Account;
import com.personalfinance.BudgetManager.Model.SavingsGoal;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Repositories.AccountRepository;
import com.personalfinance.BudgetManager.Repositories.SavingsGoalRepository;
import com.personalfinance.BudgetManager.Repositories.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
public class SavingsGoalService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final String DEFAULT_COLOR = "#14A36B";

    private final SavingsGoalRepository goalRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public SavingsGoalService(SavingsGoalRepository goalRepository, AccountRepository accountRepository,
                              UserRepository userRepository) {
        this.goalRepository = goalRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<SavingsGoalDTO> getGoals(Long userId) {
        LocalDate today = LocalDate.now();
        return goalRepository.findAllForUser(userId).stream().map(g -> toDTO(g, today)).toList();
    }

    @Transactional
    public SavingsGoalDTO create(Long userId, CreateSavingsGoalRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(userId));
        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        goal.setName(request.name().trim());
        goal.setTargetAmount(scale(request.targetAmount()));
        goal.setTargetDate(request.targetDate());
        goal.setColor(request.color() != null ? request.color().toUpperCase(Locale.ROOT) : DEFAULT_COLOR);
        goal.setSavedAmount(request.initialAmount() != null ? scale(request.initialAmount()) : BigDecimal.ZERO);
        if (request.accountId() != null) {
            Account account = accessibleAccount(request.accountId(), userId);
            goal.setAccount(account);
            goal.setCurrency(account.getCurrency().toUpperCase(Locale.ROOT));
        } else {
            goal.setCurrency(request.currency() == null || request.currency().isBlank()
                    ? "PLN" : request.currency().toUpperCase(Locale.ROOT));
        }
        return toDTO(goalRepository.save(goal), LocalDate.now());
    }

    @Transactional
    public SavingsGoalDTO update(Long userId, Long id, UpdateSavingsGoalRequest request) {
        SavingsGoal goal = owned(userId, id);
        if (request.name() != null && !request.name().isBlank()) goal.setName(request.name().trim());
        if (request.targetAmount() != null) goal.setTargetAmount(scale(request.targetAmount()));
        if (Boolean.TRUE.equals(request.clearTargetDate())) goal.setTargetDate(null);
        else if (request.targetDate() != null) goal.setTargetDate(request.targetDate());
        if (request.color() != null) goal.setColor(request.color().toUpperCase(Locale.ROOT));
        if (Boolean.TRUE.equals(request.clearAccount())) {
            goal.setAccount(null);
        } else if (request.accountId() != null) {
            Account account = accessibleAccount(request.accountId(), userId);
            goal.setAccount(account);
            goal.setCurrency(account.getCurrency().toUpperCase(Locale.ROOT));
        }
        return toDTO(goal, LocalDate.now());
    }

    @Transactional
    public SavingsGoalDTO contribute(Long userId, Long id, GoalContributionRequest request) {
        SavingsGoal goal = owned(userId, id);
        if (goal.getAccount() != null) {
            throw new IllegalStateException("Goal progress follows the linked account balance; make a transfer instead");
        }
        if (request.amount().signum() == 0) {
            throw new IllegalArgumentException("Amount must not be zero");
        }
        BigDecimal saved = goal.getSavedAmount().add(scale(request.amount()));
        if (saved.signum() < 0) {
            throw new IllegalArgumentException("Cannot withdraw more than saved for this goal");
        }
        goal.setSavedAmount(saved);
        return toDTO(goal, LocalDate.now());
    }

    @Transactional
    public void delete(Long userId, Long id) {
        goalRepository.delete(owned(userId, id));
    }

    private SavingsGoal owned(Long userId, Long id) {
        return goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal", id));
    }

    private Account accessibleAccount(Long accountId, Long userId) {
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new AccountException(accountId));
        if (account.getDeletedAt() != null || account.getGroup().getUsers().stream()
                .noneMatch(member -> member.getId().equals(userId))) {
            throw new AccessDeniedException("No access to this account");
        }
        if (account.isLiability()) {
            throw new IllegalArgumentException("A savings goal cannot be linked to a loan account");
        }
        return account;
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    static SavingsGoalDTO toDTO(SavingsGoal goal, LocalDate today) {
        Account account = goal.getAccount();
        BigDecimal current = account != null && account.getDeletedAt() == null ? account.getBalance() : goal.getSavedAmount();
        if (current == null) current = BigDecimal.ZERO;
        BigDecimal target = goal.getTargetAmount();
        BigDecimal remaining = target.subtract(current).max(BigDecimal.ZERO);
        BigDecimal percent = target.signum() == 0 ? HUNDRED
                : current.max(BigDecimal.ZERO).multiply(HUNDRED).divide(target, 1, RoundingMode.HALF_UP);
        boolean completed = current.compareTo(target) >= 0;

        Integer monthsLeft = null;
        BigDecimal monthlyNeeded = null;
        if (goal.getTargetDate() != null) {
            long months = ChronoUnit.MONTHS.between(today.withDayOfMonth(1), goal.getTargetDate().withDayOfMonth(1));
            monthsLeft = (int) Math.max(0, months);
            if (!completed) {
                monthlyNeeded = monthsLeft > 0
                        ? remaining.divide(BigDecimal.valueOf(monthsLeft), 2, RoundingMode.UP)
                        : remaining;
            }
        }
        return new SavingsGoalDTO(goal.getId(), goal.getName(), target, goal.getSavedAmount(), current, remaining,
                percent, goal.getCurrency(), goal.getTargetDate(), monthsLeft, monthlyNeeded,
                account != null ? account.getId() : null, account != null ? account.getName() : null,
                goal.getColor(), completed, goal.getCreatedAt());
    }
}
