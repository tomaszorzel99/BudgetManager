package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.InvitationResponse;
import com.personalfinance.BudgetManager.Exception.InvitationException;
import com.personalfinance.BudgetManager.Model.GroupInvitation;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Model.UserGroup;
import com.personalfinance.BudgetManager.Repositories.InvitationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class InvitationService {

    static final int VALID_DAYS = 1;

    private final InvitationRepository invitationRepository;
    private final UserService userService;
    private final UserGroupService userGroupService;

    public InvitationService(InvitationRepository invitationRepository,
                             UserService userService,
                             UserGroupService userGroupService) {
        this.invitationRepository = invitationRepository;
        this.userService = userService;
        this.userGroupService = userGroupService;
    }

    @Transactional
    public InvitationResponse inviteUserToGroup(String inviterEmail, Long groupId, String rawEmail) {
        User inviter = userService.getUserByEmail(inviterEmail);
        UserGroup group = userGroupService.getById(groupId);
        userGroupService.ensureUserIsMember(inviter, group);

        String email = AuthService.normalizeEmail(rawEmail);
        boolean alreadyInGroup = group.getUsers().stream()
                .anyMatch(member -> email.equalsIgnoreCase(member.getEmail()));
        if (alreadyInGroup) {
            throw InvitationException.alreadyMember();
        }

        GroupInvitation invitation = new GroupInvitation();
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setEmail(email);
        invitation.setGroup(group);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(VALID_DAYS).truncatedTo(ChronoUnit.SECONDS));
        invitation.setAccepted(false);
        invitationRepository.save(invitation);

        return new InvitationResponse(
                invitation.getToken(),
                group.getId(),
                group.getName(),
                invitation.getEmail(),
                invitation.getExpiresAt());
    }

    @Transactional
    public void acceptInvitation(String token, String userEmail) {
        GroupInvitation invitation = invitationRepository.findByToken(token.trim())
                .orElseThrow(InvitationException::invalidToken);

        if (invitation.isAccepted()) {
            throw InvitationException.alreadyAccepted();
        }
        if (invitation.getExpiresAt() == null || invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw InvitationException.expired();
        }

        User user = userService.getUserByEmail(userEmail);
        if (invitation.getEmail() == null || !invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw InvitationException.emailMismatch();
        }

        UserGroup group = invitation.getGroup();
        if (group == null) {
            throw InvitationException.invalidToken();
        }
        if (UserGroupService.isMember(user, group)) {
            throw InvitationException.alreadyMember();
        }

        user.getUserGroups().add(group);
        group.getUsers().add(user);
        invitation.setAccepted(true);
    }
}