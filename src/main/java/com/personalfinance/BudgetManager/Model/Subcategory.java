package com.personalfinance.BudgetManager.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subcategory")
@Getter
@Setter
@NoArgsConstructor
public class Subcategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Subcategory name cannot be blank")
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "system_key", length = 40, unique = true, updatable = false)
    private String systemKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private UserGroup group;

    public boolean isSystem() {
        return systemKey != null;
    }

    public boolean isUsableIn(UserGroup accountGroup) {
        return isSystem() || (group != null && accountGroup != null && group.getId().equals(accountGroup.getId()));
    }
}