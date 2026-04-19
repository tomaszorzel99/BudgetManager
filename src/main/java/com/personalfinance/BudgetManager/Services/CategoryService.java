package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.CreateCategoryRequest;
import com.personalfinance.BudgetManager.DTO.UpdateCategoryRequest;
import com.personalfinance.BudgetManager.Exception.CategoryException;
import com.personalfinance.BudgetManager.Model.Category;
import com.personalfinance.BudgetManager.Model.CategoryType;
import com.personalfinance.BudgetManager.Repositories.CategoryRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category createCategory(CreateCategoryRequest request){
        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setColor(request.getColor());
        category.setType(request.getType());
        return categoryRepository.save(category);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public List<Category> getCategoriesByType(CategoryType type) {
        return categoryRepository.findByType(type);
    }

    public Optional<Category> getCategoryById(Long id){
        return categoryRepository.findById(id);
    }

    public Category updateCategory(Long id, UpdateCategoryRequest request) throws AccessDeniedException {

        Category category = categoryRepository.findById(id).orElseThrow(() -> new CategoryException(id));

        updateCategoryWithPatch(category, request);
        return categoryRepository.save(category);
    }

    private void updateCategoryWithPatch(Category existingCategory, UpdateCategoryRequest request) {
        if (!(request.getName() == null)) {
            existingCategory.setName(request.getName());
        }
        if (!(request.getDescription() == null)) {
            existingCategory.setDescription(request.getDescription());
        }
        if (request.getCategoryType() != null) existingCategory.setType(request.getCategoryType());
        if (!(request.getColor() == null)) {
            existingCategory.setColor(request.getColor());
        }
    }

    public void deleteCategoryById(Long id){
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryException(id));
        categoryRepository.delete(category);
    }

}
