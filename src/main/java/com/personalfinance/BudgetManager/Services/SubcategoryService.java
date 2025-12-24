package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.Model.Subcategory;
import com.personalfinance.BudgetManager.Repositories.SubcategoryRepository;

import java.util.List;

public class SubcategoryService {

    private final SubcategoryRepository subcategoryRepository;

    public SubcategoryService(SubcategoryRepository subcategoryRepository) {
        this.subcategoryRepository = subcategoryRepository;
    }

    public Subcategory createSubcategory(Subcategory subcategory){
        return subcategoryRepository.save(subcategory);
    }

    public List<Subcategory> getAllSubcateries() {
        return subcategoryRepository.findAll();
    }

    public List<Subcategory> getSubcategoriesByCategoryId(Long categoryId) {
        return subcategoryRepository.findByCategoryId(categoryId);
    }
}
