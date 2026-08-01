package com.codemora.fantasy_league.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;

class JwtServiceTest {

    private static final String SECRET = "unit-test-signing-secret-well-over-32-bytes-long";

    @Test
    void generatedTokenCarriesUserIdAndRole() {
        JwtService jwtService = new JwtService(SECRET, 15);
        User user = User.builder().id(42L).username("alice").role(Role.ADMIN).build();

        String token = jwtService.generateAccessToken(user);
        Claims claims = jwtService.parseAndValidate(token);

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.get("username", String.class)).isEqualTo("alice");
    }

    @Test
    void expiredTokenFailsValidation() {
        JwtService jwtService = new JwtService(SECRET, 0);
        User user = User.builder().id(1L).username("bob").role(Role.USER).build();

        String token = jwtService.generateAccessToken(user);

        assertThatThrownBy(() -> jwtService.parseAndValidate(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        JwtService issuer = new JwtService(SECRET, 15);
        JwtService verifier = new JwtService("a-completely-different-signing-secret-32-bytes-plus", 15);
        User user = User.builder().id(1L).username("bob").role(Role.USER).build();

        String token = issuer.generateAccessToken(user);

        assertThatThrownBy(() -> verifier.parseAndValidate(token))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }
}
