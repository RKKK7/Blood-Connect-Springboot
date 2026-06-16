package com.bloodconnect.controller;

import com.bloodconnect.model.DonorProfile;
import com.bloodconnect.model.User;
import com.bloodconnect.repository.DonorProfileRepository;
import com.bloodconnect.repository.UserRepository;
import com.bloodconnect.service.AiService;
import com.bloodconnect.util.ApiException;
import com.bloodconnect.util.Presenter;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/donors")
public class DonorController {

    private final DonorProfileRepository donorRepo;
    private final UserRepository userRepo;
    private final AiService ai;
    private final Presenter present;

    public DonorController(DonorProfileRepository donorRepo, UserRepository userRepo,
                           AiService ai, Presenter present) {
        this.donorRepo = donorRepo; this.userRepo = userRepo; this.ai = ai; this.present = present;
    }

    private void requireAuth(User u) { if (u == null) throw new ApiException(401, "No token provided"); }
    private void requireAdmin(User u) {
        requireAuth(u);
        if (!"admin".equals(u.getRole())) throw new ApiException(403, "Admin access required");
    }

    private static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // GET /api/donors/nearby
    // Cached in Redis (cache "nearbyDonors", TTL 2 min). Key combines all query
    // params so different searches don't collide. Evicted whenever a donor profile
    // is updated or verified (see updateProfile / verify below).
    @Cacheable(value = "nearbyDonors",
            key = "T(java.lang.String).valueOf(#bloodType) + ':' + #lat + ':' + #lng + ':' + #radius")
    @GetMapping("/nearby")
    public List<Map<String, Object>> nearby(@RequestParam(required = false) Double lng,
                                            @RequestParam(required = false) Double lat,
                                            @RequestParam(defaultValue = "50") double radius,
                                            @RequestParam(required = false) String bloodType) {
        List<DonorProfile> donors = bloodType != null
            ? donorRepo.findByAvailableTrueAndBloodTypeIn(List.of(bloodType))
            : donorRepo.findByAvailableTrue();

        if (lng != null && lat != null) {
            final double flng = lng, flat = lat;
            donors = donors.stream()
                .filter(d -> d.getLocationLat() != null && d.getLocationLng() != null
                    && (d.getLocationLat() != 0.0 || d.getLocationLng() != 0.0))
                .filter(d -> haversine(flat, flng, d.getLocationLat(), d.getLocationLng()) <= radius)
                .sorted(Comparator.comparingDouble(d ->
                    haversine(flat, flng, d.getLocationLat(), d.getLocationLng())))
                .toList();
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (DonorProfile d : donors.stream().limit(50).toList()) {
            User u = userRepo.findById(d.getUserId()).orElse(null);
            out.add(present.donorProfile(d, u, "name", "phone", "email"));
        }
        return out;
    }

    // GET /api/donors/leaderboard
    // Read-heavy and changes infrequently -> cached in Redis (TTL 5 min).
    @Cacheable("leaderboard")
    @GetMapping("/leaderboard")
    public List<Map<String, Object>> leaderboard() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (DonorProfile d : donorRepo.findByTotalDonationsGreaterThanOrderByTotalDonationsDesc(0)
                .stream().limit(20).toList()) {
            User u = userRepo.findById(d.getUserId()).orElse(null);
            out.add(present.donorProfile(d, u, "name"));
        }
        return out;
    }

    // GET /api/donors/profile
    @GetMapping("/profile")
    public Map<String, Object> getProfile(@AuthenticationPrincipal User user) {
        requireAuth(user);
        DonorProfile p = donorRepo.findByUserId(user.getId())
            .orElseThrow(() -> new ApiException(404, "Donor profile not found"));
        return present.donorProfile(p, null);
    }

    // PUT /api/donors/profile  (upsert)
    // A profile change can affect nearby search and leaderboard ordering,
    // so evict both caches to avoid serving stale results.
    @Caching(evict = {
            @CacheEvict(value = "nearbyDonors", allEntries = true),
            @CacheEvict(value = "leaderboard", allEntries = true)
    })
    @PutMapping("/profile")
    public Map<String, Object> updateProfile(@AuthenticationPrincipal User user,
                                             @RequestBody Map<String, Object> b) {
        requireAuth(user);
        DonorProfile p = donorRepo.findByUserId(user.getId()).orElseGet(() -> {
            DonorProfile np = new DonorProfile();
            np.setUserId(user.getId());
            np.setBloodType(b.get("bloodType") != null ? b.get("bloodType").toString() : "O+");
            return np;
        });
        if (b.get("bloodType") != null) p.setBloodType(b.get("bloodType").toString());
        if (b.get("city") != null) p.setCity(b.get("city").toString());
        if (b.get("state") != null) p.setState(b.get("state").toString());
        if (b.get("isAvailable") instanceof Boolean av) p.setAvailable(av);
        if (b.get("weight") instanceof Number w) p.setWeight(w.doubleValue());
        if (b.get("age") instanceof Number a) p.setAge(a.intValue());
        if (b.get("medicalNotes") != null) p.setMedicalNotes(b.get("medicalNotes").toString());
        if (b.get("coordinates") instanceof List<?> c && c.size() == 2) {
            p.setLocationLng(((Number) c.get(0)).doubleValue());
            p.setLocationLat(((Number) c.get(1)).doubleValue());
        }
        p = donorRepo.save(p);
        return present.donorProfile(p, null);
    }

    // GET /api/donors/health-tip
    @GetMapping("/health-tip")
    public Map<String, Object> healthTip(@AuthenticationPrincipal User user) {
        requireAuth(user);
        DonorProfile p = donorRepo.findByUserId(user.getId())
            .orElseThrow(() -> new ApiException(404, "Donor profile not found"));
        return ai.generateHealthTip(p);
    }

    // GET /api/donors/all  (admin)
    @GetMapping("/all")
    public List<Map<String, Object>> all(@AuthenticationPrincipal User user) {
        requireAdmin(user);
        List<DonorProfile> donors = donorRepo.findAll(
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("createdAt")));
        List<Map<String, Object>> out = new ArrayList<>();
        for (DonorProfile d : donors) {
            User u = userRepo.findById(d.getUserId()).orElse(null);
            out.add(present.donorProfile(d, u, "name", "email", "phone"));
        }
        return out;
    }

    // PUT /api/donors/:id/verify  (admin)
    @CacheEvict(value = "nearbyDonors", allEntries = true)
    @PutMapping("/{id}/verify")
    public Map<String, Object> verify(@AuthenticationPrincipal User user, @PathVariable String id,
                                      @RequestBody Map<String, Object> b) {
        requireAdmin(user);
        DonorProfile p = donorRepo.findById(id).orElseThrow(() -> new ApiException(404, "Donor profile not found"));
        p.setVerified(Boolean.TRUE.equals(b.get("isVerified")));
        p = donorRepo.save(p);
        return present.donorProfile(p, null);
    }
}
