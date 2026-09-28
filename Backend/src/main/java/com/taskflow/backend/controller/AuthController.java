package com.taskflow.backend.controller;

import com.taskflow.backend.dto.request.LoginRequest;
import com.taskflow.backend.dto.request.RefreshRequest;
import com.taskflow.backend.dto.request.RegisterRequest;
import com.taskflow.backend.dto.response.EmailCheckResponse;
import com.taskflow.backend.dto.response.LoginResponse;
import com.taskflow.backend.dto.response.UserResponse;
import com.taskflow.backend.entity.User;
import com.taskflow.backend.enums.Role;
import com.taskflow.backend.exception.InvalidTokenException;
import com.taskflow.backend.repository.UserRepository;
import com.taskflow.backend.security.CustomUserDetails;
import com.taskflow.backend.security.JwtUtil;
import com.taskflow.backend.security.RefreshTokenService;
import com.taskflow.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          RefreshTokenService refreshTokenService,
                          UserService userService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse created = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Public. Triggers the "classic" Spring Security auth flow
    // AuthenticationManager internally calls CustomUserDetailsService +
    // PasswordEncoder; wrong credentials throw
    // BadCredentialsException before this method body even continues (Spring
    // Security maps that to 401 on its own).
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();

        String accessToken = jwtUtil.generateAccessToken(principal);
        String refreshToken = refreshTokenService.issueToken(principal.getId());

        return ResponseEntity.ok(new LoginResponse(
                principal.getId(),
                principal.getRole(),
                accessToken,
                refreshToken,
                jwtUtil.getAccessTokenExpiryMs()
        ));
    }

    // Public — deliberately. A refresh call happens precisely WHEN the access
    // token has expired, so this route can't require a valid access token to
    // reach it. Trust boundary shifts entirely onto refreshTokenService.isValid().
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        if (!refreshTokenService.isValid(request.userId(), request.refreshToken())) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired refresh token"));

        CustomUserDetails principal = new CustomUserDetails(user.getId(), user.getEmail(), null, user.getRole());

        String newAccessToken = jwtUtil.generateAccessToken(principal);
        String newRefreshToken = refreshTokenService.rotate(user.getId()); // old token invalidated by overwrite

        return ResponseEntity.ok(new LoginResponse(
                user.getId(),
                user.getRole(),
                newAccessToken,
                newRefreshToken,
                jwtUtil.getAccessTokenExpiryMs()
        ));
    }

    // Protected — requires a valid access token, since we need to know WHOSE
    // refresh token to revoke. Falls under SecurityConfig's anyRequest().authenticated().
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();

        refreshTokenService.revoke(principal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check-email")
    public ResponseEntity<EmailCheckResponse> checkEmailExists(@RequestParam String email) {
        return ResponseEntity.ok(new EmailCheckResponse(userService.checkEmailExists(email)));
    }
}