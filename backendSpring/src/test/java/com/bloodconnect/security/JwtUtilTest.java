package com.bloodconnect.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JWT signing/parsing. Uses a fixed 32+ char secret so the
 * HMAC key is valid, and a long expiry so tokens are valid during the test.
 */
class JwtUtilTest {

    private static final String SECRET =
            "test_secret_key_that_is_at_least_32_characters_long_123456";
    private static final long ONE_HOUR = 3_600_000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, ONE_HOUR);
    }

    @Test
    @DisplayName("A signed token parses back to the same user id (round-trip)")
    void signAndParseRoundTrip() {
        String userId = "user-123";
        String token = jwtUtil.sign(userId);

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.parseUserId(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("A token is a standard 3-part JWT (header.payload.signature)")
    void tokenHasThreeParts() {
        String token = jwtUtil.sign("abc");
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("A tampered token is rejected on parse")
    void tamperedTokenRejected() {
        String token = jwtUtil.sign("user-123");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtUtil.parseUserId(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("A token signed with a different secret cannot be parsed")
    void tokenFromDifferentSecretRejected() {
        JwtUtil other = new JwtUtil(
                "a_completely_different_secret_key_at_least_32_chars!!", ONE_HOUR);
        String foreignToken = other.sign("user-123");

        assertThatThrownBy(() -> jwtUtil.parseUserId(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("An already-expired token is rejected")
    void expiredTokenRejected() {
        // Negative expiry => token's exp is in the past the moment it's created.
        JwtUtil shortLived = new JwtUtil(SECRET, -1000L);
        String expired = shortLived.sign("user-123");

        assertThatThrownBy(() -> shortLived.parseUserId(expired))
                .isInstanceOf(JwtException.class);
    }
}
