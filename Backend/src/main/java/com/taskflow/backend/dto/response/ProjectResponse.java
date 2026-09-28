package com.taskflow.backend.dto.response;

import com.taskflow.backend.enums.ProjectStatus;

import java.time.Instant;

public record ProjectResponse(

        Long id,
        String title,
        String description,
        ProjectStatus status,
        UserSummaryResponse manager,
        Instant createdAt
) {
}
