package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.GroupInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<GroupInvitation, Long> {
    Optional<GroupInvitation> findByToken(String token);
}

