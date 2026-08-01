package com.codemora.fantasy_league.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.codemora.fantasy_league.auth.dto.AuthResponse;
import com.codemora.fantasy_league.auth.dto.LoginRequest;
import com.codemora.fantasy_league.auth.dto.RegisterRequest;
import com.codemora.fantasy_league.auth.dto.UserResponse;
import com.codemora.fantasy_league.common.error.ConflictException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService authService() {
        return new AuthService(userRepository, passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    void registerCreatesUserWithUserRoleAndHashedPassword() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse response = authService().register(new RegisterRequest("alice", "password123"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.role()).isEqualTo("USER");
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService().register(new RegisterRequest("alice", "password123")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void loginReturnsTokensForValidCredentials() {
        User user = User.builder().id(1L).username("alice").passwordHash("hashed").role(Role.USER).build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(refreshTokenService.issue(user)).thenReturn("refresh-token");

        AuthResponse response = authService().login(new LoginRequest("alice", "password123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = User.builder().id(1L).username("alice").passwordHash("hashed").role(Role.USER).build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), eq("hashed"))).thenReturn(false);

        assertThatThrownBy(() -> authService().login(new LoginRequest("alice", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginRejectsUnknownUsername() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService().login(new LoginRequest("ghost", "whatever")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
