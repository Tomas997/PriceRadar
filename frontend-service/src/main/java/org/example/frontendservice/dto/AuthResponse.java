package org.example.frontendservice.dto;

public record AuthResponse(
        String token,
        String username,
        String email,
        String role,
        String telegramChatId
) {}
