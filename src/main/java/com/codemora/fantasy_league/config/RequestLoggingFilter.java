package com.codemora.fantasy_league.config;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Logs exactly once per request: method, path, status, duration, and the
 * authenticated user id if present. Registered after JwtAuthenticationFilter
 * in SecurityConfig so authentication has already run by the time this logs.
 */
@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        long startMillis = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMillis = System.currentTimeMillis() - startMillis;
            log.info(
                    "http_request method={} path={} status={} duration_ms={} user_id={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMillis,
                    currentUserId());
        }
    }

    /**
     * Only JwtAuthenticationFilter sets a Long principal (the user id decoded
     * from the JWT). Spring Security's own AnonymousAuthenticationFilter always
     * populates a non-null Authentication too, with a "anonymousUser" String
     * principal -- that's not a real user id, so it's reported as null here.
     */
    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof Long userId ? userId : null;
    }
}
