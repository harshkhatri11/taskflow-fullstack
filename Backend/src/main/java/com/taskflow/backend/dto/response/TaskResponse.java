package com.taskflow.backend.dto.response;

import com.taskflow.backend.enums.TaskPriority;
import com.taskflow.backend.enums.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        ProjectSummaryResponse project,
        UserSummaryResponse assignedTo,
        Instant createdAt
) {
}
