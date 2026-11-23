package com.personalfinance.BudgetManager.Controller;


import com.personalfinance.BudgetManager.DTO.InvitationRequest;
import com.personalfinance.BudgetManager.DTO.InvitationResponse;
import com.personalfinance.BudgetManager.DTO.InvitationTokenRequest;
import com.personalfinance.BudgetManager.Services.InvitationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/invite")
    public ResponseEntity<InvitationResponse> invite(@Valid @RequestBody InvitationRequest invitationRequest,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        InvitationResponse response = invitationService.inviteUserToGroup(
                userDetails.getUsername(),
                invitationRequest.getGroupId(),
                invitationRequest.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/accept")
    public ResponseEntity<Void> accept(@Valid @RequestBody InvitationTokenRequest invitationTokenRequest,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        invitationService.acceptInvitation(invitationTokenRequest.getToken(), userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}