package com.bloodconnect.repository;

import com.bloodconnect.model.BloodRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, String>,
        JpaSpecificationExecutor<BloodRequest> {
    long countByRequesterIdAndStatus(String requesterId, String status);
    long countByStatus(String status);
    long countByUrgencyAndStatus(String urgency, String status);
    long countByStatusAndCreatedAtLessThanEqual(String status, Instant cutoff);

    List<BloodRequest> findByRequesterIdOrderByCreatedAtDesc(String requesterId);
    List<BloodRequest> findByStatusAndExpiresAtLessThanEqual(String status, Instant now);

    @Query("select distinct r.city from BloodRequest r where r.city is not null and r.city <> ''")
    List<String> findDistinctCities();
}
