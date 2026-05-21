package org.example.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterTest {

    private LoginRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new LoginRateLimiter();
    }

    @Test
    void allowsAttemptsUpToLimit() {
        for (int i = 0; i < LoginRateLimiter.MAX_FAILURES; i++) {
            rateLimiter.checkNotBlocked("test@example.com");
            rateLimiter.recordFailure("test@example.com");
        }
        // all MAX_FAILURES calls passed without exception
    }

    @Test
    void blocksAfterLimitExceeded() {
        for (int i = 0; i < LoginRateLimiter.MAX_FAILURES; i++) {
            rateLimiter.recordFailure("test@example.com");
        }

        assertThatThrownBy(() -> rateLimiter.checkNotBlocked("test@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    void differentEmailsHaveIndependentCounters() {
        for (int i = 0; i < LoginRateLimiter.MAX_FAILURES; i++) {
            rateLimiter.recordFailure("alice@example.com");
        }

        assertThatCode(() -> rateLimiter.checkNotBlocked("bob@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void checkDoesNotCountAsFailure() {
        for (int i = 0; i < LoginRateLimiter.MAX_FAILURES + 5; i++) {
            rateLimiter.checkNotBlocked("test@example.com");
        }
        // no failures recorded → no block
    }
}
