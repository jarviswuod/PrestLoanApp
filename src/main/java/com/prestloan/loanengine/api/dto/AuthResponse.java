package com.prestloan.loanengine.api.dto;

public record AuthResponse(String token, String tokenType, long expiresInSeconds) {}
