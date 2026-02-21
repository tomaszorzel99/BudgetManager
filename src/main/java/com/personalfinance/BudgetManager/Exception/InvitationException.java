package com.personalfinance.BudgetManager.Exception;

public class InvitationException extends RuntimeException {
    public InvitationException(String message) {
        super(message);
    }

    public static InvitationException invalidToken() {
        return new InvitationException("Invitation token is invalid or does not exist");
    }

    public static InvitationException alreadyAccepted() {
        return new InvitationException("This invitation has already been accepted");
    }

    public static InvitationException expired() {
        return new InvitationException("This invitation token has expired");
    }

    public static InvitationException emailMismatch() {
        return new InvitationException("This invitation was sent to a different email address");
    }

    public static InvitationException alreadyMember() {
        return new InvitationException("User is already a member of this group");
    }
}