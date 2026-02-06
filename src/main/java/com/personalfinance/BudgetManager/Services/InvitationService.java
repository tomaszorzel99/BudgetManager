package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.Model.GroupInvitation;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Model.UserGroup;
import com.personalfinance.BudgetManager.Repositories.InvitationRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;

    public InvitationService(InvitationRepository invitationRepository) {
        this.invitationRepository = invitationRepository;
    }

    @Transactional
    public String inviteUserToGroup(User inviter, UserGroup group, String email) {

        if (!inviter.getUserGroups().contains(group)) {
            throw new IllegalStateException("User is not a member of this group");
        }

        GroupInvitation invitation = new GroupInvitation();
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setEmail(email);
        invitation.setGroup(group);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(1));
        invitation.setAccepted(false);

        invitationRepository.save(invitation);

        return invitation.getToken();
    }

    @Transactional
    public void acceptInvitation(String token, User user) {
        GroupInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid Token"));

        if (invitation.isAccepted()) {
            throw new RuntimeException("Already accepted");
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        if (!invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new IllegalStateException("Invitation email mismatch");
        }

        UserGroup group = invitation.getGroup();

        if (user.getUserGroups().contains(group)) {
            throw new IllegalStateException("User already belongs to this group");
        }

        user.getUserGroups().add(group);
        group.getUsers().add(user);

        invitation.setAccepted(true);
    }
}
