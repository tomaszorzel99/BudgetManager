package com.personalfinance.BudgetManager.Mapper;

import com.personalfinance.BudgetManager.DTO.SubcategoryDTO;
import com.personalfinance.BudgetManager.Model.Subcategory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SubcategoryMapper {

    public SubcategoryDTO convertToDTO(Subcategory subcategory){
        SubcategoryDTO subcategoryDTO = new SubcategoryDTO();
        subcategoryDTO.setId(subcategory.getId());
        subcategoryDTO.setName(subcategory.getName());
        subcategoryDTO.setDescription(subcategory.getDescription());
        subcategoryDTO.setCategoryName(subcategory.getCategory().getName());
        return subcategoryDTO;
    }

    public List<SubcategoryDTO> convertToListDTO(List<Subcategory> subcategories){
        return subcategories.stream()
                .map(this::convertToDTO)
                .toList();
    }
}
