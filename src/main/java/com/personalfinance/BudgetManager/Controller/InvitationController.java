package com.personalfinance.BudgetManager.Controller;


import com.personalfinance.BudgetManager.DTO.InvitationRequest;
import com.personalfinance.BudgetManager.DTO.InvitationTokenRequest;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Model.UserGroup;
import com.personalfinance.BudgetManager.Services.InvitationService;
import com.personalfinance.BudgetManager.Services.UserGroupService;
import com.personalfinance.BudgetManager.Services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final InvitationService invitationService;
    private final UserService userService;
    private final UserGroupService userGroupService;

    public InvitationController(InvitationService invitationService, UserService userService, UserGroupService userGroupService) {
        this.invitationService = invitationService;
        this.userService = userService;
        this.userGroupService = userGroupService;
    }

    @PostMapping("/invite")
    public ResponseEntity<?> invite(@RequestBody InvitationRequest invitationRequest, @AuthenticationPrincipal UserDetails userDetails){
        User inviter = userService.getUserByEmail(userDetails.getUsername());
        UserGroup group = userGroupService.getById(invitationRequest.getGroupId());
        String token = invitationService.inviteUserToGroup(inviter, group, invitationRequest.getEmail());

        return ResponseEntity.ok(token);
    }

    @PostMapping("/accept")
    public ResponseEntity<?> accept(@RequestBody InvitationTokenRequest invitationTokenRequest, @AuthenticationPrincipal UserDetails userDetails){
        User user = userService.getUserByEmail(userDetails.getUsername());
        invitationService.acceptInvitation(invitationTokenRequest.getToken(), user);
        return ResponseEntity.ok().build();
    }
}


