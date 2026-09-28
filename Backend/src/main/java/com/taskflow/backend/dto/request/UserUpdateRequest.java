package com.taskflow.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Update payload for PUT /api/users/{id} — deliberately omits password
 * (password reset is a separate, out-of-scope concern) unlike UserRequest,
 * which create() still uses and which does require one.
 */
public record UserUpdateRequest(

        @NotBlank
        @Size(min = 3, message = "Name must be at least 3 characters")
        String name,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Pattern(regexp = "ADMIN|MANAGER|EMPLOYEE", message = "Role must be ADMIN, MANAGER, or EMPLOYEE")
        String role
) {
}