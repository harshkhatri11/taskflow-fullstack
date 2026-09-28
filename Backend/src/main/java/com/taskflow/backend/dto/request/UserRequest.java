package com.taskflow.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRequest(

        @NotBlank
        @Size(min = 3, message = "Name must be at least 3 characters")
        String name,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        @NotBlank
        @Pattern(regexp = "ADMIN|MANAGER|EMPLOYEE", message = "Role must be ADMIN, MANAGER, or EMPLOYEE")
        String role
) {
}
