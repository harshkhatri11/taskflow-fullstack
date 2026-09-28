package com.taskflow.backend.exception;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String message,
        String path,
        Map<String, String> errors
) {
    public ErrorResponse(int status, String message, String path) {
        this(Instant.now(), status, message, path, null);
    }

    public ErrorResponse(int status, String message, String path, Map<String, String> errors) {
        this(Instant.now(), status, message, path, errors);
    }
}