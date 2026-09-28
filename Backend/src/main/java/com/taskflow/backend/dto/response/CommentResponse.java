package com.taskflow.backend.dto.response;

import java.time.Instant;

public record CommentResponse(
        Long id,
        String content,
        Long authorId,
        String authorName,
        Instant createdAt
) {
}