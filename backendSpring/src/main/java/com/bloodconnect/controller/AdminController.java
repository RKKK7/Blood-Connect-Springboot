package com.bloodconnect.controller;

import com.bloodconnect.model.BloodRequest;
import com.bloodconnect.model.DonorProfile;
import com.bloodconnect.model.User;
import com.bloodconnect.repository.*;
import com.bloodconnect.util.Presenter;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepo;
    private final DonorProfileRepository donorRepo;
    private final BloodRequestRepository requestRepo;
    private final DonationRepository donationRepo;
    private final Presenter present;

    public AdminController(UserRepository userRepo, DonorProfileRepository donorRepo,
                           BloodRequestRepository requestRepo, DonationRepository donationRepo,
                           Presenter present) {
        this.userRepo = userRepo; this.donorRepo = donorRepo; this.requestRepo = requestRepo;
        this.donationRepo = donationRepo; this.present = present;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("totalUsers", userRepo.count());
        counts.put("totalDonors", donorRepo.count());
        counts.put("totalRequests", requestRepo.count());
        counts.put("openRequests", requestRepo.countByStatus("open"));
        counts.put("totalDonations", donationRepo.countByStatus("completed"));
        counts.put("criticalRequests", requestRepo.countByUrgencyAndStatus("critical", "open"));

        Map<String, Long> byType = donorRepo.findAll().stream()
            .collect(Collectors.groupingBy(DonorProfile::getBloodType, Collectors.counting()));
        List<Map<String, Object>> breakdown = byType.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .map(e -> { Map<String, Object> m = new LinkedHashMap<>();
                m.put("_id", e.getKey()); m.put("count", e.getValue()); return m; })
            .collect(Collectors.toList());

        List<BloodRequest> recent = requestRepo.findAll(Sort.by(Sort.Order.desc("createdAt")))
            .stream().limit(10).toList();
        List<Map<String, Object>> recentOut = new ArrayList<>();
        for (BloodRequest r : recent) recentOut.add(present.bloodRequestAutoRequester(r, "name"));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("stats", counts);
        res.put("bloodTypeBreakdown", breakdown);
        res.put("recentRequests", recentOut);
        return res;
    }

    @PostMapping("/donors/bulk-verify")
    public Map<String, Object> bulkVerify(@RequestBody Map<String, Object> b) {
        @SuppressWarnings("unchecked")
        List<String> donorIds = (List<String>) b.get("donorIds");
        if (donorIds == null || donorIds.isEmpty())
            return Map.of("message", "No donor IDs provided");
        boolean isVerified = Boolean.TRUE.equals(b.get("isVerified"));
        int modified = 0;
        for (String id : donorIds) {
            DonorProfile p = donorRepo.findById(id).orElse(null);
            if (p != null) { p.setVerified(isVerified); donorRepo.save(p); modified++; }
        }
        return Map.of("message", modified + " donors " + (isVerified ? "verified" : "unverified"));
    }

    @PostMapping("/requests/bulk-close")
    public Map<String, Object> bulkClose(@RequestBody Map<String, Object> b) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) b.get("requestIds");
        if (ids == null || ids.isEmpty()) return Map.of("message", "No request IDs provided");
        String status = b.get("status") != null ? b.get("status").toString() : "cancelled";
        int modified = 0;
        for (String id : ids) {
            BloodRequest r = requestRepo.findById(id).orElse(null);
            if (r != null && "open".equals(r.getStatus())) { r.setStatus(status); requestRepo.save(r); modified++; }
        }
        return Map.of("message", modified + " requests closed");
    }

    @PostMapping("/requests/close-stale")
    public Map<String, Object> closeStale() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(14));
        List<BloodRequest> stale = requestRepo.findAll().stream()
            .filter(r -> "open".equals(r.getStatus()) && r.getCreatedAt() != null
                && !r.getCreatedAt().isAfter(cutoff)).toList();
        stale.forEach(r -> { r.setStatus("expired"); requestRepo.save(r); });
        return Map.of("message", stale.size() + " stale requests closed");
    }

    @PostMapping("/seed")
    public Map<String, Object> seed() {
        requestRepo.deleteAll();
        List<User> requesters = userRepo.findAll().stream()
            .filter(u -> "requester".equals(u.getRole())).limit(3).toList();
        if (requesters.isEmpty())
            return Map.of("message", "Create at least one requester account first");

        Object[][] seed = {
            {"Arjun Mehta", "O-", 2, "Apollo Hospital", "Bengaluru", "9876543210", "critical",
                "O- blood urgently needed for cardiac surgery", "Pre-surgical requirement", "Cardiac surgery tomorrow morning"},
            {"Priya Sharma", "AB+", 1, "Manipal Hospital", "Bengaluru", "9123456780", "urgent",
                "AB+ needed for accident victim", "Road accident trauma", "Road accident victim, stable"},
            {"Ravi Kumar", "B+", 3, "St. John's", "Bengaluru", "9988776655", "normal",
                "B+ needed for dialysis patient", "Routine procedure", "Chronic kidney disease"},
            {"Sunita Rao", "A+", 2, "Fortis Hospital", "Mumbai", "9871234560", "urgent",
                "A+ needed for cancer patient", "Chemotherapy complications", "Leukemia, low platelet count"},
            {"Mohan Das", "O+", 1, "AIIMS", "Delhi", "9765432100", "normal",
                "O+ needed for hip replacement", "Elective surgery", "Hip replacement next week"}
        };
        int i = 0, created = 0;
        for (Object[] s : seed) {
            BloodRequest r = new BloodRequest();
            r.setRequesterId(requesters.get(i % requesters.size()).getId());
            r.setPatientName((String) s[0]);
            r.setBloodType((String) s[1]);
            r.setUnits((int) s[2]);
            r.setHospital((String) s[3]);
            r.setCity((String) s[4]);
            r.setContactPhone((String) s[5]);
            r.setUrgency((String) s[6]);
            r.setAiSummary((String) s[7]);
            r.setUrgencyReason((String) s[8]);
            r.setNotes((String) s[9]);
            r.setStatus("open");
            r.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
            requestRepo.save(r);
            created++; i++;
        }
        return Map.of("message", "Seeded " + created + " blood requests");
    }
}
