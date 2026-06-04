package com.bloodconnect.repository;

import com.bloodconnect.model.Donation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DonationRepository extends JpaRepository<Donation, String> {
    Optional<Donation> findByDonorIdAndRequestId(String donorId, String requestId);
    List<Donation> findByDonorIdOrderByCreatedAtDesc(String donorId);
    List<Donation> findByRequestId(String requestId);
    List<Donation> findByStatus(String status);
    long countByStatus(String status);
    List<Donation> findByStatusOrderByDonatedAtDesc(String status);
}
