package com.personalfinance.BudgetManager.Mapper;

import com.personalfinance.BudgetManager.DTO.CategoryDTO;
import com.personalfinance.BudgetManager.Model.Category;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryMapper {

    public CategoryDTO convertToDTO(Category category) {
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setId(category.getId());
        categoryDTO.setName(category.getName());
        categoryDTO.setDescription(category.getDescription());
        categoryDTO.setColor(category.getColor());
        categoryDTO.setType(category.getType());
        categoryDTO.setGroupId(category.getGroup() == null ? null : category.getGroup().getId());
        categoryDTO.setSystem(category.isSystem());
        return categoryDTO;
    }

    public List<CategoryDTO> convertToListDTO(List<Category> categories) {
        return categories.stream()
                .map(this::convertToDTO)
                .toList();
    }
}