package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.DTO.CreateSubcategoryRequest;
import com.personalfinance.BudgetManager.DTO.SubcategoryDTO;
import com.personalfinance.BudgetManager.Mapper.SubcategoryMapper;
import com.personalfinance.BudgetManager.Model.Subcategory;
import com.personalfinance.BudgetManager.Services.SubcategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/subcategories")
public class SubcategoryController {

    private final SubcategoryService subcategoryService;
    private final SubcategoryMapper subcategoryMapper;

    public SubcategoryController(SubcategoryService subcategoryService, SubcategoryMapper subcategoryMapper) {
        this.subcategoryService = subcategoryService;
        this.subcategoryMapper = subcategoryMapper;
    }

    @PostMapping
    public ResponseEntity<SubcategoryDTO> createSubcategory (@Valid @RequestBody CreateSubcategoryRequest request){
        Subcategory subcategory = subcategoryService.createSubcategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(subcategoryMapper.convertToDTO(subcategory));
    }

    @GetMapping
    public ResponseEntity<List<SubcategoryDTO>> getAllSybcategories(){
        List<Subcategory> allSubcateries = subcategoryService.getAllSubcateries();
        return ResponseEntity.ok().body(subcategoryMapper.convertToListDTO(allSubcateries));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubcategory(@PathVariable Long id){
        subcategoryService.deleteSubcategoriesById(id);
        return ResponseEntity.noContent().build();
    }
}
