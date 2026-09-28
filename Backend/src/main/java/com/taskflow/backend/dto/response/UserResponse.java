package com.taskflow.backend.dto.response;

import com.taskflow.backend.enums.Role;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        Instant createdAt,
        Long managedProjectCount,
        Long assignedTaskCount
) implements UserListItem{
}
