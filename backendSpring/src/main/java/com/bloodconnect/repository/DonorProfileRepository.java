package com.bloodconnect.repository;

import com.bloodconnect.model.DonorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DonorProfileRepository extends JpaRepository<DonorProfile, String> {
    Optional<DonorProfile> findByUserId(String userId);

    List<DonorProfile> findByAvailableTrueAndBloodTypeIn(List<String> bloodTypes);

    List<DonorProfile> findByAvailableTrue();

    List<DonorProfile> findByTotalDonationsGreaterThanOrderByTotalDonationsDesc(int n);

    List<DonorProfile> findByNextEligibleDateLessThanEqualAndReEngagementSentFalseAndAvailableFalse(Instant now);

    long countByBloodType(String bloodType);
}
