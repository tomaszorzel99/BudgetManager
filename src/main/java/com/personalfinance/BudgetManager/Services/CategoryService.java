package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.CreateCategoryRequest;
import com.personalfinance.BudgetManager.DTO.UpdateCategoryRequest;
import com.personalfinance.BudgetManager.Exception.CategoryException;
import com.personalfinance.BudgetManager.Exception.ConflictException;
import com.personalfinance.BudgetManager.Model.Category;
import com.personalfinance.BudgetManager.Model.CategoryType;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Model.UserGroup;
import com.personalfinance.BudgetManager.Repositories.BudgetRepository;
import com.personalfinance.BudgetManager.Repositories.CategoryRepository;
import com.personalfinance.BudgetManager.Repositories.RecurringTransactionRepository;
import com.personalfinance.BudgetManager.Repositories.TransactionRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class CategoryService {

    /** Kategoria z domyślnego zestawu nowej grupy. */
    private record DefaultCategory(String name, String color, CategoryType type) {
    }

    private static final List<DefaultCategory> DEFAULT_CATEGORIES = List.of(
            new DefaultCategory("Jedzenie", "#FF6A3D", CategoryType.EXPENSE),
            new DefaultCategory("Mieszkanie", "#3E7BFA", CategoryType.EXPENSE),
            new DefaultCategory("Transport", "#8B5CF6", CategoryType.EXPENSE),
            new DefaultCategory("Zdrowie", "#EC4899", CategoryType.EXPENSE),
            new DefaultCategory("Rozrywka", "#E8A10B", CategoryType.EXPENSE),
            new DefaultCategory("Inne wydatki", "#A16207", CategoryType.EXPENSE),
            new DefaultCategory("Wynagrodzenie", "#14A36B", CategoryType.INCOME),
            new DefaultCategory("Inne przychody", "#06B6D4", CategoryType.INCOME));

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final BudgetRepository budgetRepository;
    private final UserService userService;

    public CategoryService(CategoryRepository categoryRepository, TransactionRepository transactionRepository,
                           RecurringTransactionRepository recurringTransactionRepository,
                           BudgetRepository budgetRepository, UserService userService) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.budgetRepository = budgetRepository;
        this.userService = userService;
    }

    @Transactional
    public Category createCategory(CreateCategoryRequest request){
        rejectReservedType(request.getType());
        User user = userService.getCurrentUser();
        UserGroup group = resolveGroup(user, request.getGroupId());
        String name = request.getName().trim();
        if (categoryRepository.existsByGroupIdAndNameAndType(group.getId(), name, request.getType())) {
            throw new ConflictException("A category with this name and type already exists in this group");
        }
        Category category = new Category();
        category.setName(name);
        category.setDescription(request.getDescription());
        category.setColor(request.getColor().toUpperCase(Locale.ROOT));
        category.setType(request.getType());
        category.setGroup(group);
        return categoryRepository.save(category);
    }

    /** Kategorie systemowe i kategorie grup zalogowanego użytkownika. */
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findVisible(groupIds(userService.getCurrentUser()));
    }

    /** Domyślny zestaw kategorii dla nowej grupy. */
    @Transactional
    public void createDefaultCategories(UserGroup group) {
        for (DefaultCategory def : DEFAULT_CATEGORIES) {
            Category category = new Category();
            category.setName(def.name());
            category.setColor(def.color());
            category.setType(def.type());
            category.setGroup(group);
            categoryRepository.save(category);
        }
    }

    @Transactional
    public Category updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = editableCategory(id);
        CategoryType newType = request.getCategoryType();
        if (newType != null && newType != category.getType()) {
            rejectReservedType(newType);
            // Transakcje zachowują swój typ, więc po zmianie typu kategoria przestałaby do nich pasować.
            if (isInUse(category.getId())) {
                throw new ConflictException("The type of a category in use cannot be changed");
            }
        }
        String name = request.getName() == null ? category.getName() : request.getName().trim();
        CategoryType type = newType == null ? category.getType() : newType;
        if (categoryRepository.existsByGroupIdAndNameAndTypeAndIdNot(
                category.getGroup().getId(), name, type, category.getId())) {
            throw new ConflictException("A category with this name and type already exists in this group");
        }
        updateCategoryWithPatch(category, request);
        return categoryRepository.save(category);
    }

    private void updateCategoryWithPatch(Category existingCategory, UpdateCategoryRequest request) {
        if (request.getName() != null) {
            existingCategory.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            existingCategory.setDescription(request.getDescription());
        }
        if (request.getCategoryType() != null) {
            existingCategory.setType(request.getCategoryType());
        }
        if (request.getColor() != null) {
            existingCategory.setColor(request.getColor().toUpperCase(Locale.ROOT));
        }
    }

    @Transactional
    public void deleteCategoryById(Long id){
        Category category = editableCategory(id);
        if (isInUse(id)) {
            throw new ConflictException("Category is in use by transactions, budgets or recurring payments and cannot be deleted");
        }
        categoryRepository.delete(category);
    }

    /**
     * Kategoria z grupy zalogowanego użytkownika. Kategorii cudzych grup „nie ma” (404),
     * a kategorii systemowych nikt nie zmienia, bo są wspólne dla wszystkich.
     */
    private Category editableCategory(Long id) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new CategoryException(id));
        if (category.isSystem()) {
            throw new IllegalArgumentException("A system category cannot be changed or deleted");
        }
        User user = userService.getCurrentUser();
        if (category.getGroup() == null || !UserGroupService.isMember(user, category.getGroup())) {
            throw new CategoryException(id);
        }
        return category;
    }

    /**
     * Grupa nowej kategorii lub podkategorii: wskazana (użytkownik musi do niej należeć)
     * albo jedyna grupa użytkownika.
     */
    static UserGroup resolveGroup(User user, Long groupId) {
        if (groupId != null) {
            return user.getUserGroups().stream()
                    .filter(g -> g.getId().equals(groupId))
                    .findFirst()
                    .orElseThrow(() -> new AccessDeniedException("User is not member of this group"));
        }
        if (user.getUserGroups().isEmpty()) {
            throw new IllegalArgumentException("Create a group first");
        }
        if (user.getUserGroups().size() > 1) {
            throw new IllegalArgumentException("Choose a group");
        }
        return user.getUserGroups().iterator().next();
    }

    /** Id grup użytkownika do zapytań z IN; dla pustej listy -1, żeby zapytanie było poprawne. */
    static List<Long> groupIds(User user) {
        List<Long> ids = user.getUserGroups().stream().map(UserGroup::getId).toList();
        return ids.isEmpty() ? List.of(-1L) : ids;
    }

    /** Kategoria ma transakcje, budżety albo stałe wydatki. */
    private boolean isInUse(Long categoryId) {
        return transactionRepository.existsByCategoryId(categoryId)
                || budgetRepository.existsByCategoryId(categoryId)
                || recurringTransactionRepository.existsByCategoryId(categoryId);
    }

    /** Typy sald początkowych są zarezerwowane dla kategorii systemowej „Initial Balance”. */
    private static void rejectReservedType(CategoryType type) {
        if (type != null && type.isOpeningBalance()) {
            throw new IllegalArgumentException("This category type is reserved for the system");
        }
    }
}