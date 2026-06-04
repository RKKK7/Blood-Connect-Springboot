package com.bloodconnect.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "donations")
public class Donation extends BaseEntity {

    @Column(nullable = false)
    private String donorId;

    @Column(nullable = false)
    private String requestId;

    private String status = "pledged"; // pledged | confirmed | completed | cancelled
    private Instant donatedAt;

    @Column(columnDefinition = "text")
    private String notes = "";

    // Feature 9: post-donation feedback
    private Integer feedbackRating;
    private Boolean feedbackOnTime;

    @Column(columnDefinition = "text")
    private String feedbackComment = "";

    private Instant feedbackSubmittedAt;

    public String getDonorId() { return donorId; }
    public void setDonorId(String d) { this.donorId = d; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String r) { this.requestId = r; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public Instant getDonatedAt() { return donatedAt; }
    public void setDonatedAt(Instant d) { this.donatedAt = d; }
    public String getNotes() { return notes; }
    public void setNotes(String n) { this.notes = n; }
    public Integer getFeedbackRating() { return feedbackRating; }
    public void setFeedbackRating(Integer r) { this.feedbackRating = r; }
    public Boolean getFeedbackOnTime() { return feedbackOnTime; }
    public void setFeedbackOnTime(Boolean o) { this.feedbackOnTime = o; }
    public String getFeedbackComment() { return feedbackComment; }
    public void setFeedbackComment(String c) { this.feedbackComment = c; }
    public Instant getFeedbackSubmittedAt() { return feedbackSubmittedAt; }
    public void setFeedbackSubmittedAt(Instant s) { this.feedbackSubmittedAt = s; }
}
