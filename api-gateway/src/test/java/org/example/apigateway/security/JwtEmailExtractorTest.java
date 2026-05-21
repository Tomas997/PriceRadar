package org.example.apigateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtEmailExtractorTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hmac";

    private JwtEmailExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new JwtEmailExtractor();
        ReflectionTestUtils.setField(extractor, "secret", SECRET);
    }

    private String generateToken(String email) {
        return generateToken(email, SECRET, System.currentTimeMillis() + 3_600_000);
    }

    private String generateToken(String email, String secret, long expMillis) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(expMillis))
                .signWith(key)
                .compact();
    }

    @Test
    void extractEmail_withValidBearerToken_returnsEmail() {
        String token = generateToken("user@example.com");

        assertThat(extractor.extractEmail("Bearer " + token)).isEqualTo("user@example.com");
    }

    @Test
    void extractEmail_withNullHeader_returnsNull() {
        assertThat(extractor.extractEmail(null)).isNull();
    }

    @Test
    void extractEmail_withEmptyHeader_returnsNull() {
        assertThat(extractor.extractEmail("")).isNull();
    }

    @Test
    void extractEmail_withoutBearerPrefix_returnsNull() {
        String token = generateToken("user@example.com");

        assertThat(extractor.extractEmail(token)).isNull();
    }

    @Test
    void extractEmail_withMalformedToken_returnsNull() {
        assertThat(extractor.extractEmail("Bearer not.a.valid.jwt")).isNull();
    }

    @Test
    void extractEmail_withTokenSignedByDifferentKey_returnsNull() {
        String tokenWithWrongKey = generateToken("user@example.com",
                "completely-different-secret-key-for-testing", System.currentTimeMillis() + 3_600_000);

        assertThat(extractor.extractEmail("Bearer " + tokenWithWrongKey)).isNull();
    }

    @Test
    void extractEmail_withExpiredToken_returnsNull() {
        String expiredToken = generateToken("user@example.com", SECRET,
                System.currentTimeMillis() - 5_000);

        assertThat(extractor.extractEmail("Bearer " + expiredToken)).isNull();
    }

}
