package com.taskflow.backend.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record TaskRequest(

        @NotBlank
        String title,

        @NotBlank
        String description,

        @NotBlank
        @Pattern(regexp = "TODO|IN_PROGRESS|DONE", message = "Status should be TODO, IN_PROGRESS or DONE")
        String status,

        @NotBlank
        @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "Priority should be LOW, MEDIUM or HIGH")
        String priority,

        @NotNull
        Long assignedToId,

        @NotNull
        @FutureOrPresent(message = "Due date cannot be in the past")
        LocalDate dueDate

) {
}