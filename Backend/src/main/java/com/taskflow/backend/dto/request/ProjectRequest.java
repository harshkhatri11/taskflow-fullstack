package com.taskflow.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProjectRequest(

        @NotBlank
        @Size(max = 100, message = "Title must not exceed 100 characters")
        String title,

        @NotBlank
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotBlank
        @Pattern(regexp = "ACTIVE|COMPLETED|ARCHIVED", message = "Project status must be ACTIVE, COMPLETED, or ARCHIVED")
        String status,

        Long managerId
) {
}