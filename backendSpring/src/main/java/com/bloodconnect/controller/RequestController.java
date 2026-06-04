package com.bloodconnect.controller;

import com.bloodconnect.model.*;
import com.bloodconnect.repository.*;
import com.bloodconnect.service.AiService;
import com.bloodconnect.service.NotificationService;
import com.bloodconnect.service.SocketService;
import com.bloodconnect.util.ApiException;
import com.bloodconnect.util.BloodCompat;
import com.bloodconnect.util.Presenter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final BloodRequestRepository requestRepo;
    private final DonorProfileRepository donorRepo;
    private final DonationRepository donationRepo;
    private final UserRepository userRepo;
    private final AiService ai;
    private final NotificationService notifications;
    private final SocketService socket;
    private final Presenter present;

    public RequestController(BloodRequestRepository requestRepo, DonorProfileRepository donorRepo,
                             DonationRepository donationRepo, UserRepository userRepo, AiService ai,
                             NotificationService notifications, SocketService socket, Presenter present) {
        this.requestRepo = requestRepo; this.donorRepo = donorRepo; this.donationRepo = donationRepo;
        this.userRepo = userRepo; this.ai = ai; this.notifications = notifications;
        this.socket = socket; this.present = present;
    }

    private void requireAuth(User u) { if (u == null) throw new ApiException(401, "No token provided"); }

    private Map<String, Object> checkEligibility(DonorProfile p) {
        List<String> issues = new ArrayList<>();
        if (p.getWeight() > 0 && p.getWeight() < 50) issues.add("Weight must be at least 50 kg.");
        if (p.getAge() > 0 && (p.getAge() < 18 || p.getAge() > 65)) issues.add("Donors must be aged 18\u201365.");
        if (p.getLastDonated() != null) {
            long d = Duration.between(p.getLastDonated(), Instant.now()).toDays();
            if (d < 56) issues.add("Wait " + (56 - d) + " more days (56-day rule).");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("eligible", issues.isEmpty());
        m.put("issues", issues);
        return m;
    }

    private List<String> awardBadges(DonorProfile p) {
        Set<String> badges = new LinkedHashSet<>(p.getBadges());
        int t = p.getTotalDonations();
        if (t >= 1) badges.add("First Drop \uD83E\uDE78");
        if (t >= 5) badges.add("Life Saver \uD83D\uDCAA");
        if (t >= 10) badges.add("Blood Hero \uD83E\uDDB8");
        if (t >= 25) badges.add("Legend \uD83C\uDFC6");
        List<String> arr = new ArrayList<>(badges);
        p.setBadges(arr);
        donorRepo.save(p);
        return arr;
    }

    // POST /api/requests
    @PostMapping
    public Object create(@AuthenticationPrincipal User user, @RequestBody Map<String, Object> b) {
        requireAuth(user);
        String patientName = s(b, "patientName"), bloodType = s(b, "bloodType"),
            hospital = s(b, "hospital"), city = s(b, "city"), contactPhone = s(b, "contactPhone"),
            notes = s(b, "notes");
        Integer units = b.get("units") == null ? null : ((Number) b.get("units")).intValue();
        if (patientName == null || bloodType == null || units == null || hospital == null
            || city == null || contactPhone == null)
            throw new ApiException(400, "All fields required");

        if (requestRepo.countByRequesterIdAndStatus(user.getId(), "open") >= 3)
            throw new ApiException(429, "You already have 3 open requests. Please close one before posting another.");

        BloodRequest r = new BloodRequest();
        r.setRequesterId(user.getId());
        r.setPatientName(patientName);
        r.setBloodType(bloodType);
        r.setUnits(units);
        r.setHospital(hospital);
        r.setCity(city);
        r.setContactPhone(contactPhone);
        r.setNotes(notes == null ? "" : notes);
        if (b.get("coordinates") instanceof List<?> c && c.size() == 2) {
            r.setLocationLng(((Number) c.get(0)).doubleValue());
            r.setLocationLat(((Number) c.get(1)).doubleValue());
        }
        Map<String, Object> aiResult = ai.classifyUrgency(r);
        r.setUrgency((String) aiResult.get("urgency"));
        r.setUrgencyReason((String) aiResult.get("reason"));
        r.setAiSummary((String) aiResult.get("summary"));
        r.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
        r = requestRepo.save(r);

        Map<String, Object> populated = present.bloodRequest(r, user, "name");
        notifications.broadcastRequest(populated);

        List<DonorProfile> nearby = donorRepo.findByAvailableTrueAndBloodTypeIn(
            BloodCompat.compatibleDonors(bloodType));
        boolean critical = "critical".equals(aiResult.get("urgency"));
        int count = 0;
        for (DonorProfile donor : nearby) {
            if (count++ >= 10) break;
            User du = userRepo.findById(donor.getUserId()).orElse(null);
            if (du == null) continue;
            notifications.send(du.getId(),
                (critical ? "\uD83D\uDEA8 CRITICAL" : "\uD83E\uDE78 New") + " Blood Request",
                bloodType + " blood needed at " + hospital + ", " + city + ". " + aiResult.get("summary"),
                "match", "/requests/" + r.getId(), critical ? du.getEmail() : null);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("request", populated);
        res.put("aiResult", aiResult);
        return res;
    }

    // GET /api/requests
    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "open") String status,
                                    @RequestParam(required = false) String bloodType,
                                    @RequestParam(required = false) String urgency,
                                    @RequestParam(required = false) String city,
                                    @RequestParam(required = false) String search,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int limit) {
        Specification<BloodRequest> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (status != null && !status.equals("all")) ps.add(cb.equal(root.get("status"), status));
            if (bloodType != null) ps.add(cb.equal(root.get("bloodType"), bloodType));
            if (urgency != null) ps.add(cb.equal(root.get("urgency"), urgency));
            if (city != null) ps.add(cb.like(cb.lower(root.get("city")), "%" + city.toLowerCase() + "%"));
            if (search != null) {
                String like = "%" + search.toLowerCase() + "%";
                ps.add(cb.or(
                    cb.like(cb.lower(root.get("patientName")), like),
                    cb.like(cb.lower(root.get("hospital")), like),
                    cb.like(cb.lower(root.get("city")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        List<BloodRequest> all = requestRepo.findAll(spec,
            Sort.by(Sort.Order.desc("urgency"), Sort.Order.desc("createdAt")));
        long total = all.size();
        int from = Math.max(0, (page - 1) * limit);
        int to = Math.min(all.size(), from + limit);
        List<Map<String, Object>> pageItems = new ArrayList<>();
        for (BloodRequest r : (from <= to ? all.subList(from, to) : List.<BloodRequest>of()))
            pageItems.add(present.bloodRequestAutoRequester(r, "name"));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("requests", pageItems);
        res.put("total", total);
        res.put("pages", (int) Math.ceil((double) total / limit));
        res.put("page", page);
        return res;
    }

    @GetMapping("/my/requests")
    public List<Map<String, Object>> myRequests(@AuthenticationPrincipal User user) {
        requireAuth(user);
        List<Map<String, Object>> out = new ArrayList<>();
        for (BloodRequest r : requestRepo.findByRequesterIdOrderByCreatedAtDesc(user.getId()))
            out.add(present.bloodRequest(r, null));
        return out;
    }

    @GetMapping("/eligibility/check")
    public Map<String, Object> eligibility(@AuthenticationPrincipal User user) {
        requireAuth(user);
        DonorProfile p = donorRepo.findByUserId(user.getId())
            .orElseThrow(() -> new ApiException(404, "Donor profile not found"));
        Map<String, Object> elig = checkEligibility(p);
        Long daysSince = p.getLastDonated() != null
            ? Duration.between(p.getLastDonated(), Instant.now()).toDays() : null;
        Map<String, Object> res = new LinkedHashMap<>(elig);
        res.put("daysSince", daysSince);
        res.put("nextEligibleDate", p.getNextEligibleDate());
        return res;
    }

    // POST /api/requests/:id/sos
    @PostMapping("/{id}/sos")
    public Map<String, Object> sos(@AuthenticationPrincipal User user, @PathVariable String id) {
        requireAuth(user);
        BloodRequest r = requestRepo.findById(id).orElseThrow(() -> new ApiException(404, "Request not found"));
        if (!r.getRequesterId().equals(user.getId()) && !"admin".equals(user.getRole()))
            throw new ApiException(403, "Not authorized");
        if (!"open".equals(r.getStatus())) throw new ApiException(400, "Request must be open to send SOS");
        if (r.isSosSent()) throw new ApiException(400, "SOS already sent for this request");

        r.setSosSent(true);
        r.setUrgency("critical");
        requestRepo.save(r);

        List<DonorProfile> donors = donorRepo.findByAvailableTrueAndBloodTypeIn(
            BloodCompat.compatibleDonors(r.getBloodType()));
        int notified = 0;
        for (DonorProfile donor : donors) {
            User du = userRepo.findById(donor.getUserId()).orElse(null);
            if (du == null) continue;
            notifications.send(du.getId(), "\uD83D\uDEA8 EMERGENCY SOS \u2014 Blood Needed NOW",
                "URGENT: " + r.getBloodType() + " blood critically needed at " + r.getHospital() + ", "
                    + r.getCity() + " for " + r.getPatientName() + ". Please respond immediately!",
                "alert", "/requests/" + r.getId(), du.getEmail());
            notified++;
        }
        notifications.broadcastRequest(present.bloodRequest(r, null)); // new_request feed update
        socket.broadcast("sos_request", present.bloodRequest(r, null)); // io.emit("sos_request", ...)
        return Map.of("message", "SOS sent to " + notified + " compatible donors", "notified", notified);
    }

    // PUT /api/requests/donations/:donationId/confirm
    @PutMapping("/donations/{donationId}/confirm")
    public Map<String, Object> confirm(@AuthenticationPrincipal User user, @PathVariable String donationId) {
        requireAuth(user);
        Donation d = donationRepo.findById(donationId).orElseThrow(() -> new ApiException(404, "Donation not found"));
        BloodRequest req = requestRepo.findById(d.getRequestId()).orElse(null);
        if (req == null || !req.getRequesterId().equals(user.getId()))
            throw new ApiException(403, "Only requester can confirm");
        d.setStatus("confirmed");
        d = donationRepo.save(d);
        notifications.send(d.getDonorId(), "\u2705 Appointment confirmed!",
            "Please proceed to " + req.getHospital() + " to donate " + req.getBloodType() + " blood.",
            "donation", "/requests/" + req.getId());
        User donorUser = userRepo.findById(d.getDonorId()).orElse(null);
        return present.donation(d, donorUser != null ? present.userBrief(donorUser, "name") : null, null);
    }

    // PUT /api/requests/donations/:donationId/complete
    @PutMapping("/donations/{donationId}/complete")
    public Map<String, Object> complete(@AuthenticationPrincipal User user, @PathVariable String donationId) {
        requireAuth(user);
        Donation d = donationRepo.findById(donationId).orElseThrow(() -> new ApiException(404, "Donation not found"));
        BloodRequest req = requestRepo.findById(d.getRequestId()).orElse(null);
        if (req == null || (!req.getRequesterId().equals(user.getId()) && !"admin".equals(user.getRole())))
            throw new ApiException(403, "Not authorized");

        d.setStatus("completed");
        d.setDonatedAt(Instant.now());
        d = donationRepo.save(d);

        DonorProfile dp = donorRepo.findByUserId(d.getDonorId()).orElse(null);
        List<String> newBadges = new ArrayList<>();
        if (dp != null) {
            dp.setTotalDonations(dp.getTotalDonations() + 1);
            dp.setLastDonated(Instant.now());
            dp.setAvailable(false);
            dp.setNextEligibleDate(Instant.now().plus(Duration.ofDays(56)));
            dp.setReEngagementSent(false);
            dp = donorRepo.save(dp);
            newBadges = awardBadges(dp);
        }

        User donorUser = userRepo.findById(d.getDonorId()).orElse(null);
        notifications.send(d.getDonorId(), "\uD83C\uDF89 Donation complete \u2014 you're a hero!",
            "Recorded: " + req.getBloodType() + " at " + req.getHospital() + ". Total: "
                + (dp != null ? dp.getTotalDonations() : 1) + ". Badges: " + String.join(", ", newBadges),
            "donation", "/profile");
        notifications.send(req.getRequesterId(), "\u2B50 Rate your donor",
            (donorUser != null ? donorUser.getName() : "Your donor")
                + " completed their donation for " + req.getPatientName()
                + ". Please take a moment to rate your experience.",
            "system", "/requests/" + req.getId());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("donation", present.donation(d, donorUser != null ? present.userBrief(donorUser, "name") : null, null));
        res.put("newBadges", newBadges);
        return res;
    }

    // POST /api/requests/donations/:donationId/feedback
    @PostMapping("/donations/{donationId}/feedback")
    public Map<String, Object> feedback(@AuthenticationPrincipal User user, @PathVariable String donationId,
                                        @RequestBody Map<String, Object> b) {
        requireAuth(user);
        Integer rating = b.get("rating") == null ? null : ((Number) b.get("rating")).intValue();
        if (rating == null || rating < 1 || rating > 5) throw new ApiException(400, "Rating must be 1\u20135");

        Donation d = donationRepo.findById(donationId).orElseThrow(() -> new ApiException(404, "Donation not found"));
        BloodRequest req = requestRepo.findById(d.getRequestId()).orElse(null);
        if (req == null || !req.getRequesterId().equals(user.getId()))
            throw new ApiException(403, "Only requester can submit feedback");
        if (!"completed".equals(d.getStatus())) throw new ApiException(400, "Can only rate completed donations");
        if (d.getFeedbackRating() != null) throw new ApiException(409, "Feedback already submitted");

        d.setFeedbackRating(rating);
        d.setFeedbackOnTime(b.get("onTime") instanceof Boolean ? (Boolean) b.get("onTime") : null);
        d.setFeedbackComment(s(b, "comment") == null ? "" : s(b, "comment"));
        d.setFeedbackSubmittedAt(Instant.now());
        d = donationRepo.save(d);

        int adjust = rating >= 4 ? 2 : rating <= 2 ? -5 : 0;
        if (adjust != 0) {
            donorRepo.findByUserId(d.getDonorId()).ifPresent(dp -> {
                dp.setHealthScore(dp.getHealthScore() + adjust);
                donorRepo.save(dp);
            });
        }
        return present.donation(d, null, null);
    }

    // GET /api/requests/donor/history
    @GetMapping("/donor/history")
    public List<Map<String, Object>> donorHistory(@AuthenticationPrincipal User user) {
        requireAuth(user);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Donation d : donationRepo.findByDonorIdOrderByCreatedAtDesc(user.getId())) {
            BloodRequest req = requestRepo.findById(d.getRequestId()).orElse(null);
            out.add(present.donation(d, null,
                present.requestBrief(req, "bloodType", "hospital", "city", "patientName", "urgency", "createdAt")));
        }
        return out;
    }

    // GET /api/requests/:id
    @GetMapping("/{id}")
    public Map<String, Object> getOne(@PathVariable String id) {
        BloodRequest r = requestRepo.findById(id).orElseThrow(() -> new ApiException(404, "Request not found"));
        User requester = userRepo.findById(r.getRequesterId()).orElse(null);
        List<Map<String, Object>> donations = new ArrayList<>();
        for (Donation d : donationRepo.findByRequestId(id)) {
            User donorUser = userRepo.findById(d.getDonorId()).orElse(null);
            donations.add(present.donation(d, donorUser != null ? present.userBrief(donorUser, "name") : null, null));
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("request", present.bloodRequest(r, requester, "name", "phone", "email"));
        res.put("donations", donations);
        return res;
    }

    // POST /api/requests/:id/respond
    @PostMapping("/{id}/respond")
    public Donation respond(@AuthenticationPrincipal User user, @PathVariable String id) {
        requireAuth(user);
        BloodRequest r = requestRepo.findById(id).orElseThrow(() -> new ApiException(404, "Request not found"));
        if (!"open".equals(r.getStatus())) throw new ApiException(400, "Request not open");

        DonorProfile dp = donorRepo.findByUserId(user.getId()).orElse(null);
        if (dp != null) {
            Map<String, Object> elig = checkEligibility(dp);
            if (!(Boolean) elig.get("eligible"))
                throw new ApiException(400, "Not eligible", Map.of(
                    "eligibilityIssues", elig.get("issues"), "notEligible", true));
        }
        if (donationRepo.findByDonorIdAndRequestId(user.getId(), id).isPresent())
            throw new ApiException(409, "Already responded");

        Donation d = new Donation();
        d.setDonorId(user.getId());
        d.setRequestId(id);
        d.setStatus("pledged");
        d = donationRepo.save(d);

        if (!r.getRespondedDonors().contains(user.getId())) {
            r.getRespondedDonors().add(user.getId());
            requestRepo.save(r);
        }
        User requester = userRepo.findById(r.getRequesterId()).orElse(null);
        notifications.send(r.getRequesterId(), "Donor responded!",
            user.getName() + " pledged " + r.getBloodType() + " blood for " + r.getPatientName() + ".",
            "donation", "/requests/" + r.getId(), requester != null ? requester.getEmail() : null);
        return d;
    }

    // PUT /api/requests/:id/status
    @PutMapping("/{id}/status")
    public Map<String, Object> updateStatus(@AuthenticationPrincipal User user, @PathVariable String id,
                                            @RequestBody Map<String, Object> b) {
        requireAuth(user);
        BloodRequest r = requestRepo.findById(id).orElseThrow(() -> new ApiException(404, "Request not found"));
        if (!r.getRequesterId().equals(user.getId()) && !"admin".equals(user.getRole()))
            throw new ApiException(403, "Not authorized");
        String status = s(b, "status");
        r.setStatus(status);
        if ("fulfilled".equals(status)) r.setFulfilledAt(Instant.now());
        r = requestRepo.save(r);
        return present.bloodRequest(r, null);
    }

    private static String s(Map<String, Object> b, String k) {
        Object v = b.get(k); return v == null ? null : v.toString();
    }
}
