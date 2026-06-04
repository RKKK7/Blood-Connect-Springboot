package com.bloodconnect.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "blood_requests")
public class BloodRequest extends BaseEntity {

    @Column(nullable = false)
    private String requesterId;

    @Column(nullable = false)
    private String patientName;

    @Column(nullable = false)
    private String bloodType;

    @Column(nullable = false)
    private int units;

    @Column(nullable = false)
    private String hospital;

    @Column(nullable = false)
    private String city;

    private Double locationLng = 0.0;
    private Double locationLat = 0.0;

    @Column(nullable = false)
    private String contactPhone;

    private String urgency = "normal";      // critical | urgent | normal

    @Column(columnDefinition = "text")
    private String urgencyReason = "";

    private String status = "open";          // open | fulfilled | cancelled | expired

    @Column(columnDefinition = "text")
    private String aiSummary = "";

    @Column(columnDefinition = "text")
    private String notes = "";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "request_responded_donors", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "donor_user_id")
    private List<String> respondedDonors = new ArrayList<>();

    private Instant fulfilledAt;
    private Instant expiresAt;
    private boolean sosSent = false;

    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String r) { this.requesterId = r; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String p) { this.patientName = p; }
    public String getBloodType() { return bloodType; }
    public void setBloodType(String b) { this.bloodType = b; }
    public int getUnits() { return units; }
    public void setUnits(int u) { this.units = u; }
    public String getHospital() { return hospital; }
    public void setHospital(String h) { this.hospital = h; }
    public String getCity() { return city; }
    public void setCity(String c) { this.city = c; }
    public Double getLocationLng() { return locationLng; }
    public void setLocationLng(Double v) { this.locationLng = v; }
    public Double getLocationLat() { return locationLat; }
    public void setLocationLat(Double v) { this.locationLat = v; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String c) { this.contactPhone = c; }
    public String getUrgency() { return urgency; }
    public void setUrgency(String u) { this.urgency = u; }
    public String getUrgencyReason() { return urgencyReason; }
    public void setUrgencyReason(String u) { this.urgencyReason = u; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String a) { this.aiSummary = a; }
    public String getNotes() { return notes; }
    public void setNotes(String n) { this.notes = n; }
    public List<String> getRespondedDonors() { return respondedDonors; }
    public void setRespondedDonors(List<String> r) { this.respondedDonors = r; }
    public Instant getFulfilledAt() { return fulfilledAt; }
    public void setFulfilledAt(Instant f) { this.fulfilledAt = f; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant e) { this.expiresAt = e; }
    public boolean isSosSent() { return sosSent; }
    public void setSosSent(boolean v) { this.sosSent = v; }
}
