package com.personalfinance.BudgetManager.DTO;

import com.personalfinance.BudgetManager.Model.Category;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateSubcategoryRequst {

    private String name;
    private String description;
}
