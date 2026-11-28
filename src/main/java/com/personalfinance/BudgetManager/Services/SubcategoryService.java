package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.CreateSubcategoryRequest;
import com.personalfinance.BudgetManager.DTO.UpdateSubcategoryRequest;
import com.personalfinance.BudgetManager.Exception.CategoryException;
import com.personalfinance.BudgetManager.Exception.SubcategoryException;
import com.personalfinance.BudgetManager.Model.Category;
import com.personalfinance.BudgetManager.Model.Subcategory;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Model.UserGroup;
import com.personalfinance.BudgetManager.Repositories.CategoryRepository;
import com.personalfinance.BudgetManager.Repositories.RecurringTransactionRepository;
import com.personalfinance.BudgetManager.Repositories.SubcategoryRepository;
import com.personalfinance.BudgetManager.Repositories.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubcategoryService {

    private final SubcategoryRepository subcategoryRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final UserService userService;

    public SubcategoryService(SubcategoryRepository subcategoryRepository, CategoryRepository categoryRepository,
                              TransactionRepository transactionRepository,
                              RecurringTransactionRepository recurringTransactionRepository,
                              UserService userService) {
        this.subcategoryRepository = subcategoryRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.userService = userService;
    }

    /**
     * Podkategoria należy do grupy swojej kategorii. Pod kategorią systemową (np. „Korekta salda”)
     * należy do grupy wskazanej w żądaniu albo jedynej grupy użytkownika.
     */
    @Transactional
    public Subcategory createSubcategory(CreateSubcategoryRequest request){
        User user = userService.getCurrentUser();
        Category category = visibleCategory(user, request.getCategoryId());
        UserGroup group = category.isSystem()
                ? CategoryService.resolveGroup(user, request.getGroupId())
                : category.getGroup();

        Subcategory subcategory = new Subcategory();
        subcategory.setName(request.getName().trim());
        subcategory.setCategory(category);
        subcategory.setDescription(request.getDescription());
        subcategory.setGroup(group);
        return subcategoryRepository.save(subcategory);
    }

    /** Podkategorie systemowe i podkategorie grup zalogowanego użytkownika. */
    @Transactional(readOnly = true)
    public List<Subcategory> getAllSubcateries() {
        return subcategoryRepository.findVisible(CategoryService.groupIds(userService.getCurrentUser()));
    }

    @Transactional
    public Subcategory updateSubcategory(Long id, UpdateSubcategoryRequest request) {
        User user = userService.getCurrentUser();
        Subcategory subcategory = editableSubcategory(user, id);

        if (request.getCategoryId() != null && !request.getCategoryId().equals(subcategory.getCategory().getId())) {
            moveToCategory(user, subcategory, request.getCategoryId());
        }
        if (request.getName() != null) {
            subcategory.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            subcategory.setDescription(request.getDescription());
        }
        return subcategoryRepository.save(subcategory);
    }

    /**
     * Przenosi podkategorię do innej kategorii tego samego typu i tej samej grupy (albo do kategorii
     * systemowej). Transakcje i stałe wydatki z tą podkategorią przechodzą razem z nią, żeby kategoria
     * i podkategoria wpisu dalej do siebie pasowały.
     */
    private void moveToCategory(User user, Subcategory subcategory, Long targetCategoryId) {
        Category target = visibleCategory(user, targetCategoryId);
        if (target.getType() != subcategory.getCategory().getType()) {
            throw new IllegalArgumentException("A subcategory can only be moved to a category of the same type");
        }
        if (!target.isUsableIn(subcategory.getGroup())) {
            throw new IllegalArgumentException("A subcategory can only be moved to a category of the same group");
        }
        transactionRepository.moveSubcategoryToCategory(subcategory.getId(), target);
        recurringTransactionRepository.moveSubcategoryToCategory(subcategory.getId(), target);
        subcategory.setCategory(target);
    }

    @Transactional
    public void deleteSubcategoriesById(Long id){
        Subcategory subcategory = editableSubcategory(userService.getCurrentUser(), id);
        subcategoryRepository.delete(subcategory);
    }

    /** Kategoria systemowa albo kategoria grupy użytkownika; innych „nie ma” (404). */
    private Category visibleCategory(User user, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryException(categoryId));
        if (!category.isSystem()
                && (category.getGroup() == null || !UserGroupService.isMember(user, category.getGroup()))) {
            throw new CategoryException(categoryId);
        }
        return category;
    }

    /** Podkategoria z grupy użytkownika; systemowe są wspólne, więc nikt ich nie zmienia. */
    private Subcategory editableSubcategory(User user, Long id) {
        Subcategory subcategory = subcategoryRepository.findById(id).orElseThrow(() -> new SubcategoryException(id));
        if (subcategory.isSystem()) {
            throw new IllegalArgumentException("A system subcategory cannot be changed or deleted");
        }
        if (subcategory.getGroup() == null || !UserGroupService.isMember(user, subcategory.getGroup())) {
            throw new SubcategoryException(id);
        }
        return subcategory;
    }
}