package org.example.userservice.dto;

import org.example.userservice.model.User;

public record UserResponse(
        Long id,
        String username,
        String email,
        String role
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getDisplayName(), user.getEmail(), user.getRole().name());
    }
}
