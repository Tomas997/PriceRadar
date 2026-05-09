package org.example.frontendservice.dto;

public record UserResponse(
        Long id,
        String username,
        String email,
        String role,
        String telegramChatId
) {}
