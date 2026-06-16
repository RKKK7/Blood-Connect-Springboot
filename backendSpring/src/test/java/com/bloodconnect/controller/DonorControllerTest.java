package com.bloodconnect.controller;

import com.bloodconnect.model.DonorProfile;
import com.bloodconnect.model.User;
import com.bloodconnect.repository.DonorProfileRepository;
import com.bloodconnect.repository.UserRepository;
import com.bloodconnect.service.AiService;
import com.bloodconnect.util.Presenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

/**
 * Mockito unit tests for DonorController. Focus on the two bits of real logic
 * the controller owns: leaderboard mapping and the Haversine radius filter in
 * /nearby. Repositories, AI, and the presenter are all mocked.
 */
@ExtendWith(MockitoExtension.class)
class DonorControllerTest {

    @Mock DonorProfileRepository donorRepo;
    @Mock UserRepository userRepo;
    @Mock AiService ai;
    @Mock Presenter present;

    private DonorController controller;

    @BeforeEach
    void setUp() {
        controller = new DonorController(donorRepo, userRepo, ai, present);
    }

    private DonorProfile donor(String userId, double lat, double lng) {
        DonorProfile d = new DonorProfile();
        d.setId("d-" + userId);
        d.setUserId(userId);
        d.setBloodType("O+");
        d.setLocationLat(lat);
        d.setLocationLng(lng);
        return d;
    }

    @Test
    @DisplayName("leaderboard maps each top donor through the presenter")
    void leaderboardReturnsMappedDonors() {
        when(donorRepo.findByTotalDonationsGreaterThanOrderByTotalDonationsDesc(0))
                .thenReturn(List.of(donor("u1", 0, 0), donor("u2", 0, 0)));
        when(userRepo.findById(anyString())).thenReturn(Optional.of(new User()));
        when(present.donorProfile(any(DonorProfile.class), nullable(User.class), any(String[].class)))
                .thenReturn(Map.of("name", "Donor"));

        List<Map<String, Object>> result = controller.leaderboard();

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("nearby filters out donors outside the radius (Haversine)")
    void nearbyFiltersByRadius() {
        DonorProfile near = donor("u-near", 12.9716, 77.5946); // Bengaluru
        DonorProfile far  = donor("u-far", 28.7041, 77.1025);  // Delhi (~1700 km)

        when(donorRepo.findByAvailableTrue()).thenReturn(List.of(near, far));
        when(userRepo.findById(anyString())).thenReturn(Optional.of(new User()));
        when(present.donorProfile(any(DonorProfile.class), nullable(User.class), any(String[].class)))
                .thenReturn(Map.of("name", "Near Donor"));

        // Search around Bengaluru, 50 km radius, no blood-type filter.
        List<Map<String, Object>> result =
                controller.nearby(77.59, 12.97, 50, null);

        assertThat(result).hasSize(1); // only the Bengaluru donor survives
    }
}
