package com.schoolsaas.auth.dto;

import com.schoolsaas.auth.User;

public record UserResponse(
        Long id, String email, String firstName, String lastName, String role, boolean active) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.isActive());
    }
}
