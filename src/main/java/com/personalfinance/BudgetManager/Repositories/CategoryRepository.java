package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.Category;
import com.personalfinance.BudgetManager.Model.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findById(Long id);
    Optional<Category> findBySystemKey(String systemKey);

    @Query("SELECT c FROM Category c WHERE c.systemKey IS NOT NULL OR c.group.id IN :groupIds ORDER BY c.name")
    List<Category> findVisible(@Param("groupIds") Collection<Long> groupIds);

    boolean existsByGroupIdAndNameAndType(Long groupId, String name, CategoryType type);

    boolean existsByGroupIdAndNameAndTypeAndIdNot(Long groupId, String name, CategoryType type, Long id);
}