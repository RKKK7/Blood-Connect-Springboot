package com.bloodconnect.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "donor_profiles", indexes = @Index(name = "idx_donor_user", columnList = "userId", unique = true))
public class DonorProfile extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String userId;

    @Column(nullable = false)
    private String bloodType;

    // Geo: stored as plain coordinates (lng, lat). Nearby search via Haversine.
    private Double locationLng = 0.0;
    private Double locationLat = 0.0;

    private String city = "";
    private String state = "";
    private boolean available = true;
    private Instant lastDonated;
    private int totalDonations = 0;
    private boolean isVerified = false;
    private int healthScore = 100;
    private double weight = 0;
    private int age = 0;

    @Column(columnDefinition = "text")
    private String medicalNotes = "";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "donor_badges", joinColumns = @JoinColumn(name = "donor_id"))
    @Column(name = "badge")
    private List<String> badges = new ArrayList<>();

    // Feature 3: re-engagement
    private Instant nextEligibleDate;
    private boolean reEngagementSent = false;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }
    public Double getLocationLng() { return locationLng; }
    public void setLocationLng(Double v) { this.locationLng = v; }
    public Double getLocationLat() { return locationLat; }
    public void setLocationLat(Double v) { this.locationLat = v; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean v) { this.available = v; }
    public Instant getLastDonated() { return lastDonated; }
    public void setLastDonated(Instant d) { this.lastDonated = d; }
    public int getTotalDonations() { return totalDonations; }
    public void setTotalDonations(int t) { this.totalDonations = t; }
    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean v) { this.isVerified = v; }
    public int getHealthScore() { return healthScore; }
    public void setHealthScore(int h) { this.healthScore = h; }
    public double getWeight() { return weight; }
    public void setWeight(double w) { this.weight = w; }
    public int getAge() { return age; }
    public void setAge(int a) { this.age = a; }
    public String getMedicalNotes() { return medicalNotes; }
    public void setMedicalNotes(String m) { this.medicalNotes = m; }
    public List<String> getBadges() { return badges; }
    public void setBadges(List<String> b) { this.badges = b; }
    public Instant getNextEligibleDate() { return nextEligibleDate; }
    public void setNextEligibleDate(Instant d) { this.nextEligibleDate = d; }
    public boolean isReEngagementSent() { return reEngagementSent; }
    public void setReEngagementSent(boolean v) { this.reEngagementSent = v; }
}
