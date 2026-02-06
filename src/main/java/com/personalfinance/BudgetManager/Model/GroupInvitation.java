package com.personalfinance.BudgetManager.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "groupInvitation")
@Getter
@Setter
@NoArgsConstructor
public class GroupInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Email
    private String email;

    @ManyToOne
    private UserGroup group;

    private LocalDateTime expiresAt;

    private boolean accepted = false;

}
