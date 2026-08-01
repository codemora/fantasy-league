package com.codemora.fantasy_league.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, 30);
    }

    @Test
    void issueReturnsPlaintextButPersistsOnlyTheHash() {
        User user = User.builder().id(1L).username("alice").role(Role.USER).build();

        String plaintext = refreshTokenService.issue(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertThat(plaintext).isNotBlank();
        assertThat(saved.getTokenHash()).isNotEqualTo(plaintext);
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void redeemReturnsUserAndRevokesTheToken() {
        User user = User.builder().id(1L).username("alice").role(Role.USER).build();
        RefreshToken stored = RefreshToken.builder()
                .user(user)
                .tokenHash("irrelevant-in-this-test")
                .expiresAt(Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS))
                .build();

        // Capture the hash issue() computed, so redeem() looks it up successfully.
        String plaintext = refreshTokenService.issue(user);
        ArgumentCaptor<RefreshToken> issuedCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(issuedCaptor.capture());
        stored.setTokenHash(issuedCaptor.getValue().getTokenHash());

        when(refreshTokenRepository.findByTokenHash(stored.getTokenHash())).thenReturn(Optional.of(stored));

        User redeemed = refreshTokenService.redeem(plaintext);

        assertThat(redeemed).isEqualTo(user);
        assertThat(stored.getRevokedAt()).isNotNull();
    }

    @Test
    void redeemRejectsUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.redeem("not-a-real-token"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void redeemRejectsExpiredToken() {
        User user = User.builder().id(1L).username("alice").role(Role.USER).build();
        RefreshToken expired = RefreshToken.builder()
                .user(user)
                .tokenHash("some-hash")
                .expiresAt(Instant.now().minusSeconds(1))
                .build();
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.redeem("expired-token"))
                .isInstanceOf(BadCredentialsException.class);
    }
}
