package com.codemora.fantasy_league.config;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * The JWT filter puts the authenticated user's id directly on the SecurityContext
 * as the principal (see JwtAuthenticationFilter) -- this just gives feature code a
 * typed way to read it back, e.g. for stamping created_by_user_id.
 */
@Component
public class CurrentUserProvider {

    public Long getUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
