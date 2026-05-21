package org.example.userservice.service;

import org.example.userservice.model.RefreshToken;
import org.example.userservice.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository repo;
    @InjectMocks private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "refreshExpirationMs", 604800000L); // 7 days
    }

    private RefreshToken savedToken(String tokenValue, String email) {
        return new RefreshToken(tokenValue, email, LocalDateTime.now().plusDays(7));
    }

    // ─── create ─────────────────────────────────────────────────────────────

    @Test
    void create_deletesExistingTokensForUser_beforeCreatingNew() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create("alice@example.com");

        verify(repo).deleteByUserEmail("alice@example.com");
    }

    @Test
    void create_savesTokenWithCorrectEmail() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create("alice@example.com");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getUserEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void create_savesTokenWithFutureExpiry() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create("alice@example.com");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void create_savesNonBlankToken() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create("alice@example.com");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getToken()).isNotBlank();
    }

    // ─── rotate ─────────────────────────────────────────────────────────────

    @Test
    void rotate_deletesOldToken_andCreatesNew() {
        RefreshToken existing = savedToken("old-token", "alice@example.com");
        when(repo.findByToken("old-token")).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.rotate("old-token");

        verify(repo).delete(existing);
        verify(repo).deleteByUserEmail("alice@example.com");
        verify(repo).save(any());
    }

    @Test
    void rotate_returnsNewTokenWithSameEmail() {
        RefreshToken existing = savedToken("old-token", "alice@example.com");
        when(repo.findByToken("old-token")).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = service.rotate("old-token");

        assertThat(result.getUserEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void rotate_throwsUnauthorized_whenTokenNotFound() {
        when(repo.findByToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("unknown"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void rotate_throwsUnauthorized_andDeletesExpiredToken() {
        RefreshToken expired = new RefreshToken("expired-token", "alice@example.com",
                LocalDateTime.now().minusHours(1));
        when(repo.findByToken("expired-token")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.rotate("expired-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(repo).delete(expired);
    }

    // ─── revoke ─────────────────────────────────────────────────────────────

    @Test
    void revoke_deletesTokenByValue() {
        service.revoke("some-token");

        verify(repo).deleteByToken("some-token");
    }

    @Test
    void revoke_doesNotThrow_whenTokenDoesNotExist() {
        doNothing().when(repo).deleteByToken(any());

        // Should complete without throwing
        service.revoke("nonexistent-token");
    }
}
