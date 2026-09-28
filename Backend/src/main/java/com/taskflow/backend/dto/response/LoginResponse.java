package com.taskflow.backend.dto.response;

import com.taskflow.backend.enums.Role;

public record LoginResponse(
        Long id,
        Role role,
        String accessToken,
        String refreshToken,
        Long expiresIn
) {
}