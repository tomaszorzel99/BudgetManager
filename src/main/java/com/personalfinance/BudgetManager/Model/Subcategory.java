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

    /** Klucz podkategorii systemowej (np. LOAN_INTEREST); null dla podkategorii użytkownika. */
    @Column(name = "system_key", length = 40, unique = true, updatable = false)
    private String systemKey;

    /**
     * Grupa, do której należy podkategoria (ta sama co kategorii, a pod kategorią systemową
     * grupa, która ją dodała); null dla podkategorii systemowych.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private UserGroup group;

    public boolean isSystem() {
        return systemKey != null;
    }

    /** Podkategorię systemową można użyć wszędzie, podkategorię grupy tylko na kontach tej grupy. */
    public boolean isUsableIn(UserGroup accountGroup) {
        return isSystem() || (group != null && accountGroup != null && group.getId().equals(accountGroup.getId()));
    }
}