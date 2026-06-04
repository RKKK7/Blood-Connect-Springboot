package com.bloodconnect.service;

import com.bloodconnect.model.BloodRequest;
import com.bloodconnect.model.DonorProfile;
import com.bloodconnect.model.User;
import com.bloodconnect.repository.BloodRequestRepository;
import com.bloodconnect.repository.DonorProfileRepository;
import com.bloodconnect.repository.UserRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Feature 4: auto-expire requests after 7 days.
 * Feature 3: re-engage eligible donors after 56 days.
 * Runs immediately on startup, then hourly.
 */
@Service
public class CronService {

    private static final long HOUR = 60L * 60L * 1000L;

    private final BloodRequestRepository requestRepo;
    private final DonorProfileRepository donorRepo;
    private final UserRepository userRepo;
    private final NotificationService notifications;

    public CronService(BloodRequestRepository requestRepo, DonorProfileRepository donorRepo,
                       UserRepository userRepo, NotificationService notifications) {
        this.requestRepo = requestRepo;
        this.donorRepo = donorRepo;
        this.userRepo = userRepo;
        this.notifications = notifications;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        expireOldRequests();
        reEngageDonors();
        System.out.println("[CRON] Jobs started: request expiry + donor re-engagement");
    }

    @Scheduled(fixedRate = HOUR)
    public void expireOldRequests() {
        try {
            List<BloodRequest> expired = requestRepo
                .findByStatusAndExpiresAtLessThanEqual("open", Instant.now());
            for (BloodRequest req : expired) {
                req.setStatus("expired");
                requestRepo.save(req);
                notifications.send(req.getRequesterId(), "\u23F0 Request expired",
                    "Your blood request for " + req.getPatientName() + " (" + req.getBloodType()
                        + ") at " + req.getHospital() + " has expired after 7 days. Please post a new request if still needed.",
                    "alert", "/requests/new");
            }
            if (!expired.isEmpty())
                System.out.println("[CRON] Expired " + expired.size() + " old blood requests");
        } catch (Exception e) {
            System.err.println("[CRON] expireOldRequests error: " + e.getMessage());
        }
    }

    @Scheduled(fixedRate = HOUR)
    public void reEngageDonors() {
        try {
            List<DonorProfile> donors = donorRepo
                .findByNextEligibleDateLessThanEqualAndReEngagementSentFalseAndAvailableFalse(Instant.now());
            for (DonorProfile donor : donors) {
                User u = userRepo.findById(donor.getUserId()).orElse(null);
                if (u == null) continue;
                notifications.send(u.getId(), "\uD83E\uDE78 You're eligible to donate again!",
                    "Hi " + u.getName() + "! It's been 56+ days since your last donation. You're now eligible to donate blood again. Turn on your availability and save a life today!",
                    "system", "/profile", u.getEmail());
                donor.setReEngagementSent(true);
                donor.setAvailable(true);
                donorRepo.save(donor);
            }
            if (!donors.isEmpty())
                System.out.println("[CRON] Re-engaged " + donors.size() + " donors");
        } catch (Exception e) {
            System.err.println("[CRON] reEngageDonors error: " + e.getMessage());
        }
    }
}
