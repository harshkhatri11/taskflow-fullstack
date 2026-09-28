package com.taskflow.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Thrown by service layer for "not found" AND "not-your-resource" cases
    // (deliberate — see ProjectServiceImpl.getProjectById's documented asymmetry)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // Thrown by Spring itself when no controller mapping matches the requested path
    // at all (typo'd URL, missing path segment, wrong method+path combo). Distinct
    // from ResourceNotFoundException above: that one means "a real endpoint ran and
    // the resource wasn't found"; this one means "there was no endpoint to run in
    // the first place." Genuinely a 404 either way from the client's perspective.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "The requested endpoint does not exist",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // Thrown when the request path matches a real mapping but not for this HTTP
    // verb (e.g. POST to a path that only has a GET handler). Path is real, verb
    // isn't — 405, not 404 and not 500. getSupportedMethods() comes straight from
    // the matched @RequestMapping(s), so the message is accurate per-endpoint
    // rather than a generic string.
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String supported = ex.getSupportedMethods() != null
                ? String.join(", ", ex.getSupportedMethods())
                : "none";
        String message = String.format(
                "%s method is not supported for this endpoint. Supported methods: %s",
                ex.getMethod(),
                supported
        );
        ErrorResponse body = new ErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    // Thrown when the client's Content-Type header doesn't match what the endpoint
    // accepts (e.g. sending form-urlencoded or text/plain to a @RequestBody
    // expecting application/json). Client-side header mistake, not a server fault.
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                "Unsupported content type — expected application/json",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
    }

    // Thrown when a required @RequestParam is missing entirely from the query
    // string (e.g. a paginated endpoint expecting ?page= and it's just absent —
    // distinct from a present-but-wrong-type param, which is the next handler).
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Missing required parameter: " + ex.getParameterName(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Thrown when a path variable or query param is present but the wrong type —
    // e.g. GET /api/projects/abc where {id} expects a Long. Same family of bug as
    // the refresh-token String-instead-of-Long issue you hit earlier, but that one
    // was a REQUEST BODY field (caught by HttpMessageNotReadableException below);
    // this is the PATH/QUERY equivalent.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid value for parameter: " + ex.getName(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Thrown when @Validated (not @Valid) constraints fail on method params directly
    // — e.g. a @RequestParam or @PathVariable annotated with @Min/@Positive rather
    // than a full request-body DTO. MethodArgumentNotValidException (below) covers
    // @Valid on @RequestBody; this covers the narrower param-level case.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Thrown by the DB driver when an operation violates a constraint — most
    // relevant case in this project: deleting a Project/User/Task that still has
    // dependent rows under a RESTRICT foreign key. This is exactly the concern
    // flagged in postman-test-plan.md test #54 ("confirm cascade/block behavior
    // fires, not a raw FK constraint 500") — this handler is what makes that test
    // pass cleanly instead of leaking a Hibernate/JDBC stack trace. 409 Conflict:
    // the request is well-formed, but conflicts with current DB state.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "This action conflicts with existing related data (e.g. dependent records still exist)",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // Thrown for bad-input / business-rule violations (e.g. wrong role supplying
    // managerId, non-EMPLOYEE passed as assignee) — see Project/Task/Comment impls
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Thrown when the request body can't be parsed into the target DTO at all —
    // malformed JSON, wrong type for a field (e.g. a non-numeric string where a
    // Long is expected), truncated body. A parsing failure is always a client
    // mistake, never a server fault, so this is a 400, not a 500.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Malformed request body",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Thrown by Spring Security when @PreAuthorize evaluates to false
    // (role gate fails, or a security-bean ownership check returns false)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "You do not have permission to perform this action",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // Thrown by AuthenticationManager.authenticate() inside AuthController.login()
    // when credentials are wrong (BadCredentialsException) or the account is
    // otherwise unusable (DisabledException, LockedException, etc.) — all extend
    // AuthenticationException. Unlike the filter-layer 401 (JwtAuthenticationEntryPoint,
    // fires for a missing/invalid ACCESS token on a protected route and never reaches
    // DispatcherServlet), this is thrown from INSIDE a controller method during the
    // manual authenticate() call, so it's squarely inside the MVC pipeline and
    // @RestControllerAdvice catches it like any other service-layer exception.
    // Generic message deliberately reused for every subtype — same "don't leak
    // existence" posture as the wrong-password-vs-no-such-user case.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Invalid email or password",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // Thrown when @Valid fails on a request DTO (@NotBlank, @Size, @Pattern, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Thrown by AuthController.refresh() when the presented refresh token doesn't
    // match what's stored in Redis (or has expired via TTL). Unlike the filter-layer
    // 401 (JwtAuthenticationEntryPoint — missing/expired ACCESS token, never reaches
    // DispatcherServlet), this happens INSIDE the controller's own logic, so
    // @RestControllerAdvice can catch it normally here.
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }


    // Thrown by Spring's exception-translation layer for ANY failure talking to a
    // backing store it doesn't own the root cause of — Redis connection refused,
    // Redis command timeout, a JDBC access failure, etc. This is the SHARED PARENT
    // of many specific exceptions (RedisConnectionFailureException,
    // QueryTimeoutException, and others) — catching it here means we don't need a
    // separate @ExceptionHandler for every individual subtype Lettuce/JDBC might
    // throw depending on exactly HOW the dependency failed (refused vs. timed out
    // vs. reset). The caller doesn't need to know which subtype fired, just that
    // WE couldn't reach something we depend on — that's always a 503 ("try again
    // later"), never a 500 ("something is broken in our own logic").
    //
    // DataIntegrityViolationException above is a MORE SPECIFIC subtype of this
    // same DataAccessException family, and Spring always dispatches to the closest
    // match, so it still correctly wins (409) for constraint violations — this
    // handler only catches what nothing more specific already claimed.
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessFailure(DataAccessException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "A required service is temporarily unavailable. Please try again shortly.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    // Catch-all safety net — anything unanticipated becomes a clean 500 instead
    // of leaking a stack trace to the client. Should now genuinely only fire for
    // things nobody could have predicted, not routine client mistakes.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}