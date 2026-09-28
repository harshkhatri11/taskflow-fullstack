package com.taskflow.backend.dto.response;

import com.taskflow.backend.enums.Role;

import java.time.Instant;

public record UserSummaryResponse(
        Long id,
        String name,
        Role role
) implements UserListItem{
}
