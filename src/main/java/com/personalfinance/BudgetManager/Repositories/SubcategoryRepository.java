package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {
    List<Subcategory> findByCategoryName(String categoryName);
    Optional<Subcategory> findById(Long id);
}
