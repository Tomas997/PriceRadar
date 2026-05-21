package org.example.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "test-secret-key-that-is-long-enough-for-hmac");
        ReflectionTestUtils.setField(jwtService, "accessExpirationMs", 3600000L);
    }

    @Test
    void generateAccessToken_returnsThreePartJwt() {
        String token = jwtService.generateAccessToken("user@example.com", "USER");

        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        String token = jwtService.generateAccessToken("user@example.com", "USER");

        assertThat(jwtService.extractEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    void isValid_returnsTrueForFreshToken() {
        String token = jwtService.generateAccessToken("user@example.com", "USER");

        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void isValid_returnsFalseForTamperedSignature() {
        String token = jwtService.generateAccessToken("user@example.com", "USER");
        String tampered = token.substring(0, token.lastIndexOf('.')) + ".invalidsig";

        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    void isValid_returnsFalseForExpiredToken() {
        JwtService shortLivedService = new JwtService();
        ReflectionTestUtils.setField(shortLivedService, "secret", "test-secret-key-that-is-long-enough-for-hmac");
        ReflectionTestUtils.setField(shortLivedService, "accessExpirationMs", -1000L);

        String token = shortLivedService.generateAccessToken("user@example.com", "USER");

        assertThat(shortLivedService.isValid(token)).isFalse();
    }

    @Test
    void isValid_returnsFalseForMalformedToken() {
        assertThat(jwtService.isValid("not.a.valid.jwt.token")).isFalse();
        assertThat(jwtService.isValid("onlytwoparts.here")).isFalse();
        assertThat(jwtService.isValid("")).isFalse();
    }

    @Test
    void generateAccessToken_differentEmailsProduceDifferentTokens() {
        String token1 = jwtService.generateAccessToken("alice@example.com", "USER");
        String token2 = jwtService.generateAccessToken("bob@example.com", "USER");

        assertThat(token1).isNotEqualTo(token2);
    }
}
