package com.bloodconnect.model;

import jakarta.persistence.*;

@Entity
@Table(name = "platform_stats")
public class PlatformStats extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String date; // YYYY-MM-DD

    private int totalRequests = 0;
    private int fulfilledReqs = 0;
    private int totalDonations = 0;
    private int criticalSaved = 0;

    public String getDate() { return date; }
    public void setDate(String d) { this.date = d; }
    public int getTotalRequests() { return totalRequests; }
    public void setTotalRequests(int t) { this.totalRequests = t; }
    public int getFulfilledReqs() { return fulfilledReqs; }
    public void setFulfilledReqs(int f) { this.fulfilledReqs = f; }
    public int getTotalDonations() { return totalDonations; }
    public void setTotalDonations(int t) { this.totalDonations = t; }
    public int getCriticalSaved() { return criticalSaved; }
    public void setCriticalSaved(int c) { this.criticalSaved = c; }
}
