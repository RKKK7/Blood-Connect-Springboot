package com.bloodconnect.controller;

import com.bloodconnect.model.BloodRequest;
import com.bloodconnect.model.DonorProfile;
import com.bloodconnect.model.User;
import com.bloodconnect.repository.BloodRequestRepository;
import com.bloodconnect.repository.DonorProfileRepository;
import com.bloodconnect.repository.UserRepository;
import com.bloodconnect.service.AiService;
import com.bloodconnect.util.ApiException;
import com.bloodconnect.util.BloodCompat;
import com.bloodconnect.util.Presenter;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/match")
public class MatchController {

    private final BloodRequestRepository requestRepo;
    private final DonorProfileRepository donorRepo;
    private final UserRepository userRepo;
    private final AiService ai;
    private final Presenter present;

    public MatchController(BloodRequestRepository requestRepo, DonorProfileRepository donorRepo,
                           UserRepository userRepo, AiService ai, Presenter present) {
        this.requestRepo = requestRepo; this.donorRepo = donorRepo; this.userRepo = userRepo;
        this.ai = ai; this.present = present;
    }

    // GET /api/match/:requestId
    @GetMapping("/{requestId}")
    public List<Map<String, Object>> match(@PathVariable String requestId) {
        BloodRequest request = requestRepo.findById(requestId)
            .orElseThrow(() -> new ApiException(404, "Request not found"));

        List<DonorProfile> donors = donorRepo.findByAvailableTrueAndBloodTypeIn(
            BloodCompat.compatibleDonors(request.getBloodType())).stream().limit(20).toList();

        List<Map<String, Object>> scored = new ArrayList<>();
        for (DonorProfile donor : donors) {
            Map<String, Object> aiScore = ai.scoreDonorMatch(donor, request);
            User u = userRepo.findById(donor.getUserId()).orElse(null);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("donor", present.donorProfile(donor, u, "name", "phone", "city"));
            entry.put("aiScore", aiScore);
            scored.add(entry);
        }
        scored.sort((a, b) -> {
            int sa = ((Number) ((Map<?, ?>) a.get("aiScore")).get("score")).intValue();
            int sb = ((Number) ((Map<?, ?>) b.get("aiScore")).get("score")).intValue();
            return sb - sa;
        });
        return scored;
    }
}
