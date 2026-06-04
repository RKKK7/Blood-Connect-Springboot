package com.bloodconnect.repository;

import com.bloodconnect.model.PlatformStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformStatsRepository extends JpaRepository<PlatformStats, String> {
    Optional<PlatformStats> findByDate(String date);
}
