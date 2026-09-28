package com.taskflow.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.backend.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Fires when an unauthenticated request hits a protected route — missing,
 * malformed, or expired token. This runs OUTSIDE the DispatcherServlet
 * pipeline (see the note on GlobalExceptionHandler's scope) so it needs its
 * own wiring here, into SecurityConfig, to produce the same ErrorResponse
 * shape rather than Spring Security's default plain-text 403 page.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        ErrorResponse body = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Missing or invalid authentication token",
                request.getRequestURI()
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}