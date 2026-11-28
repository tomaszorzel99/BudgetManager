package com.personalfinance.BudgetManager.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_category_group_name_type",
                columnNames = {"group_id", "name", "type"}))
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Category name cannot be blank")
    @Column(nullable = false)
    private String name;

    private String description;

    @Column(length = 7, nullable = false)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type;

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