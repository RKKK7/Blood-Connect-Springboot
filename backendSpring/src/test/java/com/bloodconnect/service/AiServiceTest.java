package com.bloodconnect.service;

import com.bloodconnect.model.BloodRequest;
import com.bloodconnect.model.DonorProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests AiService's resilience contract: when the Groq API is unreachable,
 * every method must return safe, documented fallback values instead of throwing.
 *
 * We point the client at an unroutable local address so the HTTP call fails
 * fast and the catch/fallback branch is exercised - no real network needed.
 */
class AiServiceTest {

    private AiService aiService;

    @BeforeEach
    void setUp() {
        // Port 1 is reserved and refuses connections immediately -> fast failure.
        aiService = new AiService(
                "",                                  // empty api key
                "llama-3.1-8b-instant",
                "http://127.0.0.1:1/v1/chat/completions");
    }

    @Test
    @DisplayName("classifyUrgency falls back to 'normal' when Groq is unreachable")
    void classifyUrgencyFallback() {
        BloodRequest r = new BloodRequest();
        r.setPatientName("Asha");
        r.setBloodType("O-");
        r.setUnits(2);
        r.setHospital("City Hospital");

        Map<String, Object> result = aiService.classifyUrgency(r);

        assertThat(result.get("urgency")).isEqualTo("normal");
        assertThat(result.get("summary").toString()).contains("City Hospital");
    }

    @Test
    @DisplayName("scoreDonorMatch falls back to a neutral compatible score of 50")
    void scoreDonorMatchFallback() {
        DonorProfile donor = new DonorProfile();
        donor.setBloodType("O-");
        BloodRequest request = new BloodRequest();
        request.setBloodType("O-");

        Map<String, Object> result = aiService.scoreDonorMatch(donor, request);

        assertThat(result.get("score")).isEqualTo(50);
        assertThat(result.get("compatible")).isEqualTo(true);
    }

    @Test
    @DisplayName("generateHealthTip falls back to a default hydration tip")
    void healthTipFallback() {
        DonorProfile p = new DonorProfile();
        p.setBloodType("A+");

        Map<String, Object> result = aiService.generateHealthTip(p);

        assertThat(result.get("title")).isEqualTo("Post-donation care");
        assertThat(result.get("tip").toString()).isNotBlank();
    }
}
