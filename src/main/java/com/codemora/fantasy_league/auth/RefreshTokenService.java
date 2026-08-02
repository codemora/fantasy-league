package com.codemora.fantasy_league.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

/**
 * Issues, redeems, and revokes the opaque refresh tokens described in ADR 0008.
 * Only a SHA-256 hash of each token is ever persisted -- the plaintext token is
 * returned to the caller once, at issue time, and never stored.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenTtlDays;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    public String issue(User user) {
        String plaintext = generatePlaintextToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(plaintext))
                .expiresAt(Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(refreshToken);
        return plaintext;
    }

    /**
     * Validates a refresh token and rotates it (marks it revoked) in one step.
     * The caller is expected to issue a fresh replacement immediately after.
     */
    public User redeem(String plaintextToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(plaintextToken))
                .filter(RefreshToken::isValid)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));
        refreshToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);
        return refreshToken.getUser();
    }

    public void revoke(String plaintextToken) {
        refreshTokenRepository.findByTokenHash(hash(plaintextToken))
                .ifPresent(token -> {
                    token.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(token);
                });
    }

    private String generatePlaintextToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String plaintextToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(plaintextToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
