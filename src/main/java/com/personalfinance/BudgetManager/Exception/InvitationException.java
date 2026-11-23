package com.personalfinance.BudgetManager.Exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvitationException extends RuntimeException {

    private final HttpStatus status;

    public InvitationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static InvitationException invalidToken() {
        return new InvitationException(HttpStatus.NOT_FOUND, "Invitation token is invalid or does not exist");
    }

    public static InvitationException alreadyAccepted() {
        return new InvitationException(HttpStatus.CONFLICT, "This invitation has already been accepted");
    }

    public static InvitationException expired() {
        return new InvitationException(HttpStatus.GONE, "This invitation token has expired");
    }

    public static InvitationException emailMismatch() {
        return new InvitationException(HttpStatus.FORBIDDEN, "This invitation was sent to a different email address");
    }

    public static InvitationException alreadyMember() {
        return new InvitationException(HttpStatus.CONFLICT, "User is already a member of this group");
    }
}