package com.bloodconnect.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for the blood-type compatibility matrix.
 * No Spring context, no database - fast and deterministic.
 */
class BloodCompatTest {

    @Test
    @DisplayName("O- is the universal donor: only O- can receive O-")
    void universalDonorReceivesOnlyOMinus() {
        assertThat(BloodCompat.compatibleDonors("O-"))
                .containsExactlyInAnyOrder("O-");
    }

    @Test
    @DisplayName("AB+ is the universal recipient: accepts every blood type")
    void universalRecipientAcceptsAll() {
        assertThat(BloodCompat.compatibleDonors("AB+"))
                .containsExactlyInAnyOrder(
                        "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
    }

    @ParameterizedTest(name = "{0} can receive from {1} donor types")
    @CsvSource({
            "A+, 4",
            "A-, 2",
            "B+, 4",
            "B-, 2",
            "AB+, 8",
            "AB-, 4",
            "O+, 2",
            "O-, 1"
    })
    @DisplayName("Each recipient type maps to the correct number of compatible donors")
    void compatibleDonorCounts(String recipient, int expectedCount) {
        assertThat(BloodCompat.compatibleDonors(recipient)).hasSize(expectedCount);
    }

    @Test
    @DisplayName("Every compatible donor list always includes O- (universal donor)")
    void everyRecipientCanReceiveOMinus() {
        for (String type : List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")) {
            assertThat(BloodCompat.compatibleDonors(type))
                    .as("recipient %s should accept O-", type)
                    .contains("O-");
        }
    }

    @Test
    @DisplayName("An unknown blood type falls back to itself rather than throwing")
    void unknownTypeFallsBackToItself() {
        assertThat(BloodCompat.compatibleDonors("XYZ"))
                .containsExactly("XYZ");
    }
}
