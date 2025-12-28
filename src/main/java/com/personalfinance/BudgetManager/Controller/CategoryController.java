package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.DTO.CategoryDTO;
import com.personalfinance.BudgetManager.DTO.CreateCategoryRequest;
import com.personalfinance.BudgetManager.Mapper.CategoryMapper;
import com.personalfinance.BudgetManager.Model.Category;
import com.personalfinance.BudgetManager.Services.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    public CategoryController(CategoryService categoryService, CategoryMapper categoryMapper) {
        this.categoryService = categoryService;
        this.categoryMapper = categoryMapper;
    }

    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CreateCategoryRequest request){
        Category category = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryMapper.convertToDTO(category));
    }

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories(){
        List<Category> allCategories = categoryService.getAllCategories();
        return ResponseEntity.ok().body(categoryMapper.convertToListDTO(allCategories));
    }
}
