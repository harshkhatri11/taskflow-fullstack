package com.taskflow.backend.security;

import com.taskflow.backend.enums.Role;
import org.springframework.security.core.Authentication;

/**
 * Single source of truth for pulling principal fields out of the SecurityContext's
 * Authentication object. Every security bean (@projectSecurity, @taskSecurity,
 * @commentSecurity) calls this instead of casting authentication.getPrincipal()
 * itself — keeps the cast and the "what if it's malformed" handling in one place.
 *
 * Not a @Component: no dependencies to inject, and static keeps call sites
 * (AuthUtil.extractUserId(authentication)) simple inside SpEL-adjacent plain-Java
 * helper methods.
 */
public final class AuthUtil {

    private AuthUtil() {
        // static utility, never instantiated
    }

    public static Long extractUserId(Authentication authentication) {
        return principal(authentication).getId();
    }

    public static Role extractRole(Authentication authentication) {
        return principal(authentication).getRole();
    }

    public static String extractEmail(Authentication authentication) {
        // CustomUserDetails has no dedicated getEmail() — email doubles as the
        // UserDetails username, so getUsername() is the correct accessor here.
        return principal(authentication).getUsername();
    }

    private static CustomUserDetails principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            // Fail loud, not silent-null. If this ever fires, JwtAuthenticationFilter
            // isn't populating SecurityContextHolder correctly upstream — that's a
            // config bug worth a stack trace, not a quietly-false ownership check.
            throw new IllegalStateException("No authenticated CustomUserDetails principal found on Authentication");
        }
        return details;
    }
}