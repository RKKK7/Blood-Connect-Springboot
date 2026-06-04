package com.bloodconnect.util;

import com.bloodconnect.model.*;
import com.bloodconnect.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Builds JSON maps that match the original Mongoose/Express responses exactly,
 * so the React frontend (which reads _id, populated nested objects, etc.) works unchanged.
 */
@Component
public class Presenter {

    private final UserRepository userRepository;

    public Presenter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Auth payload user object: { id, _id, name, email, role, phone }. Frontend reads user.id. */
    public Map<String, Object> userAuth(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("_id", u.getId());
        m.put("name", u.getName());
        m.put("email", u.getEmail());
        m.put("role", u.getRole());
        m.put("phone", u.getPhone());
        return m;
    }

    /** Full user object for /me (no password). */
    public Map<String, Object> userFull(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("_id", u.getId());
        m.put("id", u.getId());
        m.put("name", u.getName());
        m.put("email", u.getEmail());
        m.put("phone", u.getPhone());
        m.put("role", u.getRole());
        m.put("avatar", u.getAvatar());
        Map<String, Object> sched = new LinkedHashMap<>();
        sched.put("enabled", u.isScheduleEnabled());
        sched.put("days", u.getScheduleDays());
        sched.put("startTime", u.getScheduleStartTime());
        sched.put("endTime", u.getScheduleEndTime());
        m.put("availabilitySchedule", sched);
        m.put("openRequestCount", u.getOpenRequestCount());
        m.put("createdAt", u.getCreatedAt());
        m.put("updatedAt", u.getUpdatedAt());
        return m;
    }

    /** Populated user subset, e.g. populate("requesterId","name phone email"). */
    public Map<String, Object> userBrief(User u, String... fields) {
        if (u == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("_id", u.getId());
        m.put("id", u.getId());
        Set<String> f = new HashSet<>(Arrays.asList(fields));
        if (f.isEmpty() || f.contains("name")) m.put("name", u.getName());
        if (f.contains("email")) m.put("email", u.getEmail());
        if (f.contains("phone")) m.put("phone", u.getPhone());
        if (f.contains("city")) m.put("city", "");
        return m;
    }

    public Map<String, Object> userBriefById(String userId, String... fields) {
        if (userId == null) return null;
        return userRepository.findById(userId).map(u -> userBrief(u, fields)).orElse(null);
    }

    private Map<String, Object> location(Double lng, Double lat) {
        Map<String, Object> loc = new LinkedHashMap<>();
        loc.put("type", "Point");
        loc.put("coordinates", List.of(lng == null ? 0.0 : lng, lat == null ? 0.0 : lat));
        return loc;
    }

    /** Donor profile. If populatedUser != null, userId becomes a nested object. */
    public Map<String, Object> donorProfile(DonorProfile p, User populatedUser, String... userFields) {
        if (p == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("_id", p.getId());
        m.put("id", p.getId());
        m.put("userId", populatedUser != null ? userBrief(populatedUser, userFields) : p.getUserId());
        m.put("bloodType", p.getBloodType());
        m.put("location", location(p.getLocationLng(), p.getLocationLat()));
        m.put("city", p.getCity());
        m.put("state", p.getState());
        m.put("isAvailable", p.isAvailable());
        m.put("lastDonated", p.getLastDonated());
        m.put("totalDonations", p.getTotalDonations());
        m.put("isVerified", p.isVerified());
        m.put("healthScore", p.getHealthScore());
        m.put("weight", p.getWeight());
        m.put("age", p.getAge());
        m.put("medicalNotes", p.getMedicalNotes());
        m.put("badges", p.getBadges());
        m.put("nextEligibleDate", p.getNextEligibleDate());
        m.put("reEngagementSent", p.isReEngagementSent());
        m.put("createdAt", p.getCreatedAt());
        m.put("updatedAt", p.getUpdatedAt());
        return m;
    }

    /** Blood request. If requester != null, requesterId becomes a nested object. */
    public Map<String, Object> bloodRequest(BloodRequest r, User requester, String... requesterFields) {
        if (r == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("_id", r.getId());
        m.put("id", r.getId());
        m.put("requesterId", requester != null ? userBrief(requester, requesterFields) : r.getRequesterId());
        m.put("patientName", r.getPatientName());
        m.put("bloodType", r.getBloodType());
        m.put("units", r.getUnits());
        m.put("hospital", r.getHospital());
        m.put("city", r.getCity());
        m.put("location", location(r.getLocationLng(), r.getLocationLat()));
        m.put("contactPhone", r.getContactPhone());
        m.put("urgency", r.getUrgency());
        m.put("urgencyReason", r.getUrgencyReason());
        m.put("status", r.getStatus());
        m.put("aiSummary", r.getAiSummary());
        m.put("notes", r.getNotes());
        m.put("respondedDonors", r.getRespondedDonors());
        m.put("fulfilledAt", r.getFulfilledAt());
        m.put("expiresAt", r.getExpiresAt());
        m.put("sosSent", r.isSosSent());
        m.put("createdAt", r.getCreatedAt());
        m.put("updatedAt", r.getUpdatedAt());
        return m;
    }

    public Map<String, Object> bloodRequestAutoRequester(BloodRequest r, String... requesterFields) {
        User requester = r.getRequesterId() != null
                ? userRepository.findById(r.getRequesterId()).orElse(null) : null;
        return bloodRequest(r, requester, requesterFields);
    }

    private Map<String, Object> feedback(Donation d) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("rating", d.getFeedbackRating());
        f.put("onTime", d.getFeedbackOnTime());
        f.put("comment", d.getFeedbackComment());
        f.put("submittedAt", d.getFeedbackSubmittedAt());
        return f;
    }

    /** Donation. donorObj / requestObj when populated; else raw id strings. */
    public Map<String, Object> donation(Donation d, Object donorObj, Object requestObj) {
        if (d == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("_id", d.getId());
        m.put("id", d.getId());
        m.put("donorId", donorObj != null ? donorObj : d.getDonorId());
        m.put("requestId", requestObj != null ? requestObj : d.getRequestId());
        m.put("status", d.getStatus());
        m.put("donatedAt", d.getDonatedAt());
        m.put("notes", d.getNotes());
        m.put("feedback", feedback(d));
        m.put("createdAt", d.getCreatedAt());
        m.put("updatedAt", d.getUpdatedAt());
        return m;
    }

    /** Subset of a request used when populating donation.requestId. */
    public Map<String, Object> requestBrief(BloodRequest r, String... fields) {
        if (r == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("_id", r.getId());
        m.put("id", r.getId());
        Set<String> f = new HashSet<>(Arrays.asList(fields));
        if (f.isEmpty() || f.contains("bloodType")) m.put("bloodType", r.getBloodType());
        if (f.isEmpty() || f.contains("hospital")) m.put("hospital", r.getHospital());
        if (f.contains("city")) m.put("city", r.getCity());
        if (f.contains("patientName")) m.put("patientName", r.getPatientName());
        if (f.contains("urgency")) m.put("urgency", r.getUrgency());
        if (f.contains("createdAt")) m.put("createdAt", r.getCreatedAt());
        return m;
    }

    public Map<String, Object> chatMessage(ChatMessage c, User sender) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("_id", c.getId());
        m.put("id", c.getId());
        m.put("donationId", c.getDonationId());
        m.put("senderId", sender != null ? userBrief(sender, "name") : c.getSenderId());
        m.put("receiverId", c.getReceiverId());
        m.put("message", c.getMessage());
        m.put("isRead", c.isRead());
        m.put("createdAt", c.getCreatedAt());
        m.put("updatedAt", c.getUpdatedAt());
        return m;
    }

    public Map<String, Object> notification(Notification n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("_id", n.getId());
        m.put("id", n.getId());
        m.put("userId", n.getUserId());
        m.put("title", n.getTitle());
        m.put("message", n.getMessage());
        m.put("type", n.getType());
        m.put("isRead", n.isRead());
        m.put("link", n.getLink());
        m.put("createdAt", n.getCreatedAt());
        m.put("updatedAt", n.getUpdatedAt());
        return m;
    }
}
