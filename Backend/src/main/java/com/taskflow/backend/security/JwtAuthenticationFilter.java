package com.taskflow.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return; // no token — let it through; SecurityConfig decides if the route needs auth
        }

        String token = authHeader.substring(7);

        if (jwtUtil.isTokenValid(token)) {
            Long userId = jwtUtil.extractUserId(token);
            String email = jwtUtil.extractEmail(token);
            var role = jwtUtil.extractRole(token);

            // Built directly from JWT claims — deliberately NOT a DB call via
            // CustomUserDetailsService here. That service is only for login's
            // initial password check; every subsequent request trusts the
            // signed token instead, which is the whole performance point of JWT.
            CustomUserDetails principal = new CustomUserDetails(userId, email, null, role);

            var authToken = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
        // invalid/expired token: leave SecurityContext empty — anonymous.
        // SecurityConfig + the entry point below turn that into a 401 for
        // any route that actually requires authentication.

        filterChain.doFilter(request, response);
    }
}