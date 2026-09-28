package com.taskflow.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CommentRequest(

        @NotBlank
        String content

) {
}
