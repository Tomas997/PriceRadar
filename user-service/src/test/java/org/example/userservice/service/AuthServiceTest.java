package org.example.userservice.service;

import org.example.userservice.dto.AuthResponse;
import org.example.userservice.dto.LoginRequest;
import org.example.userservice.dto.RegisterRequest;
import org.example.userservice.exception.UserAlreadyExistsException;
import org.example.userservice.model.RefreshToken;
import org.example.userservice.model.Role;
import org.example.userservice.model.User;
import org.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private LoginRateLimiter loginRateLimiter;
    @Mock private RefreshTokenService refreshTokenService;
    @InjectMocks private AuthService authService;

    private User sampleUser() {
        User u = new User("Alice", "alice@example.com", "hashed", Role.USER);
        u.setId(1L);
        return u;
    }

    private RefreshToken sampleRefreshToken() {
        return new RefreshToken("refresh-uuid", "alice@example.com", LocalDateTime.now().plusDays(7));
    }

    // ─── register ───────────────────────────────────────────────────────────

    @Test
    void register_savesUserAndReturnsBothTokens() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser());
        when(jwtService.generateAccessToken("alice@example.com", "USER")).thenReturn("access-token");
        when(refreshTokenService.create("alice@example.com")).thenReturn(sampleRefreshToken());

        AuthResponse response = authService.register(
                new RegisterRequest("Alice", "alice@example.com", "password123", null));

        assertThat(response.token()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-uuid");
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.role()).isEqualTo("USER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_passwordIsHashed_notStoredPlaintext() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedvalue");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(any(), any())).thenReturn("token");
        when(refreshTokenService.create(any())).thenReturn(sampleRefreshToken());

        authService.register(new RegisterRequest("Alice", "alice@example.com", "password123", null));

        verify(userRepository).save(argThat(u -> u.getPassword().equals("$2a$10$hashedvalue")));
    }

    @Test
    void register_throwsConflict_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Alice", "alice@example.com", "password123", null)))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("alice@example.com");

        verify(userRepository, never()).save(any());
    }

    // ─── login ──────────────────────────────────────────────────────────────

    @Test
    void login_returnsBothTokens_whenCredentialsValid() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser()));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken("alice@example.com", "USER")).thenReturn("access-token");
        when(refreshTokenService.create("alice@example.com")).thenReturn(sampleRefreshToken());

        AuthResponse response = authService.login(new LoginRequest("alice@example.com", "password123"));

        assertThat(response.token()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-uuid");
        assertThat(response.username()).isEqualTo("Alice");
    }

    @Test
    void login_throwsBadCredentials_whenUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown@example.com", "password")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_throwsBadCredentials_whenPasswordWrong() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser()));
        when(passwordEncoder.matches("wrongpassword", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "wrongpassword")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_sameErrorForWrongUserAndWrongPassword() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser()));
        when(passwordEncoder.matches("wrongpassword", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("nonexistent@example.com", "pass")))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "wrongpassword")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_throwsTooManyRequests_whenRateLimitExceeded() {
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed login attempts"))
                .when(loginRateLimiter).checkNotBlocked("alice@example.com");

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "password")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    void login_recordsFailure_whenUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown@example.com", "password")))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginRateLimiter).recordFailure("unknown@example.com");
    }

    @Test
    void login_recordsFailure_whenPasswordWrong() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser()));
        when(passwordEncoder.matches("wrongpassword", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "wrongpassword")))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginRateLimiter).recordFailure("alice@example.com");
    }

    @Test
    void login_doesNotRecordFailure_whenCredentialsValid() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser()));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(any(), any())).thenReturn("token");
        when(refreshTokenService.create(any())).thenReturn(sampleRefreshToken());

        authService.login(new LoginRequest("alice@example.com", "password123"));

        verify(loginRateLimiter, never()).recordFailure(any());
    }

    // ─── refresh ────────────────────────────────────────────────────────────

    @Test
    void refresh_returnsNewTokens_whenRefreshTokenValid() {
        RefreshToken newRefresh = new RefreshToken("new-refresh", "alice@example.com",
                LocalDateTime.now().plusDays(7));
        when(refreshTokenService.rotate("old-refresh")).thenReturn(newRefresh);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser()));
        when(jwtService.generateAccessToken("alice@example.com", "USER")).thenReturn("new-access");

        AuthResponse response = authService.refresh("old-refresh");

        assertThat(response.token()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        assertThat(response.email()).isEqualTo("alice@example.com");
    }

    @Test
    void refresh_throwsUnauthorized_whenRefreshTokenInvalid() {
        when(refreshTokenService.rotate("bad-token"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        assertThatThrownBy(() -> authService.refresh("bad-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void refresh_throwsUnauthorized_whenRefreshTokenExpired() {
        when(refreshTokenService.rotate("expired-token"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired"));

        assertThatThrownBy(() -> authService.refresh("expired-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    // ─── logout ─────────────────────────────────────────────────────────────

    @Test
    void logout_revokesRefreshToken() {
        authService.logout("some-refresh-token");

        verify(refreshTokenService).revoke("some-refresh-token");
    }

    @Test
    void logout_doesNotThrow_whenTokenDoesNotExist() {
        doNothing().when(refreshTokenService).revoke(any());

        authService.logout("nonexistent-token");
    }

    // ─── me ─────────────────────────────────────────────────────────────────

    @Test
    void me_returnsUserResponse_whenUserExists() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser()));

        var response = authService.me("alice@example.com");

        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.username()).isEqualTo("Alice");
        assertThat(response.role()).isEqualTo("USER");
    }

    @Test
    void loadUserByUsername_returnsUserDetails() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser()));

        var userDetails = authService.loadUserByUsername("alice@example.com");

        assertThat(userDetails.getUsername()).isEqualTo("alice@example.com");
    }
}
