package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {
    Optional<Subcategory> findById(Long id);
    Optional<Subcategory> findBySystemKey(String systemKey);

    @Query("SELECT s FROM Subcategory s JOIN FETCH s.category "
            + "WHERE s.systemKey IS NOT NULL OR s.group.id IN :groupIds ORDER BY s.name")
    List<Subcategory> findVisible(@Param("groupIds") Collection<Long> groupIds);
}