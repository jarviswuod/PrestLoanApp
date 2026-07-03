package com.prestloan.loanengine.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInSeconds
) {
}
