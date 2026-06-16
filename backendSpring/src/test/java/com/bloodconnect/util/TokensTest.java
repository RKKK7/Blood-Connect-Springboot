package com.bloodconnect.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the password-reset token helper:
 * SHA-256 hashing must be deterministic, and random tokens must be unique.
 */
class TokensTest {

    @Test
    @DisplayName("sha256Hex is deterministic: same input -> same hash")
    void sha256IsDeterministic() {
        String a = Tokens.sha256Hex("reset-me");
        String b = Tokens.sha256Hex("reset-me");
        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("sha256Hex produces a 64-char lowercase hex string")
    void sha256HasCorrectShape() {
        String hash = Tokens.sha256Hex("anything");
        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("Different inputs produce different hashes")
    void sha256DiffersForDifferentInput() {
        assertThat(Tokens.sha256Hex("a")).isNotEqualTo(Tokens.sha256Hex("b"));
    }

    @Test
    @DisplayName("randomToken is 64 hex chars (32 random bytes)")
    void randomTokenShape() {
        assertThat(Tokens.randomToken()).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("Two random tokens are practically never equal")
    void randomTokensAreUnique() {
        assertThat(Tokens.randomToken()).isNotEqualTo(Tokens.randomToken());
    }
}
