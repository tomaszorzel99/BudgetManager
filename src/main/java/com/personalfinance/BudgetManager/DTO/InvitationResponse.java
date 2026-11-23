package com.personalfinance.BudgetManager.DTO;

import java.time.LocalDateTime;

public record InvitationResponse(String token, Long groupId, String groupName, String email, LocalDateTime expiresAt) {
}