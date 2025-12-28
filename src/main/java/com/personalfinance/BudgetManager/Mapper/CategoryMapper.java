package com.personalfinance.BudgetManager.Mapper;

import com.personalfinance.BudgetManager.DTO.CategoryDTO;
import com.personalfinance.BudgetManager.Model.Category;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryMapper {

    private String mapHexToName(String hex){
        return switch (hex.toUpperCase()){
            case "FF0000" -> "RED";
            case "#00FF00" -> "GREEN";
            case "#0000FF" -> "BLUE";
            default -> "UNKNOWN";
        };
    }

    public CategoryDTO convertToDTO (Category category){
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setId(category.getId());
        categoryDTO.setName(category.getName());
        categoryDTO.setDescription(category.getDescription());
        categoryDTO.setColor(mapHexToName(category.getColor()));
        categoryDTO.setType(category.getType());
        return categoryDTO;
    }

    public List<CategoryDTO> convertToListDTO(List<Category> categories){
        return categories.stream()
                .map(this::convertToDTO)
                .toList();
    }

}
