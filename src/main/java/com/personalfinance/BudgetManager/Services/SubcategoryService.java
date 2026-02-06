package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.CreateSubcategoryRequest;
import com.personalfinance.BudgetManager.Exception.CategoryException;
import com.personalfinance.BudgetManager.Exception.SubcategoryException;
import com.personalfinance.BudgetManager.Model.Category;
import com.personalfinance.BudgetManager.Model.Subcategory;
import com.personalfinance.BudgetManager.Repositories.CategoryRepository;
import com.personalfinance.BudgetManager.Repositories.SubcategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubcategoryService {

    private final SubcategoryRepository subcategoryRepository;
    private final CategoryRepository categoryRepository;

    public SubcategoryService(SubcategoryRepository subcategoryRepository, CategoryRepository categoryRepository) {
        this.subcategoryRepository = subcategoryRepository;
        this.categoryRepository = categoryRepository;
    }

    public Subcategory createSubcategory(CreateSubcategoryRequest request){
        Category category = categoryRepository.findById(request.getCategoryId()).
                orElseThrow(() -> new CategoryException(request.getCategoryId()));

        Subcategory subcategory = new Subcategory();
        subcategory.setName(request.getName());
        subcategory.setCategory(category);
        subcategory.setDescription(request.getDescription());
        return subcategoryRepository.save(subcategory);
    }

    public List<Subcategory> getAllSubcateries() {
        return subcategoryRepository.findAll();
    }

    public List<Subcategory> getSubcategoriesByCategoryName(String categoryName) {
        return subcategoryRepository.findByCategoryName(categoryName);
    }

    public void deleteSubcategoriesById(Long id){
        Subcategory subcategory = subcategoryRepository.findById(id).orElseThrow(() -> new SubcategoryException(id));
        subcategoryRepository.delete(subcategory);
    }
}
