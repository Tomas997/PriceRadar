package org.example.userservice.dto;

public record AuthResponse(
        String token,
        String username,
        String email,
        String role,
        String telegramChatId
) {}
