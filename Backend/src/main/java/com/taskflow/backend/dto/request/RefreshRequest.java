package com.taskflow.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RefreshRequest(
        @NotNull Long userId,
        @NotBlank String refreshToken
) {
}