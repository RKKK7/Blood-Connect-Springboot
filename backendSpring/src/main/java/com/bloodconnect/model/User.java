package com.bloodconnect.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", indexes = @Index(name = "idx_user_email", columnList = "email", unique = true))
public class User extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String phone = "";

    @Column(nullable = false)
    private String role = "donor"; // donor | requester | admin

    @Column(columnDefinition = "text")
    private String avatar = "";

    // Feature 10: password reset
    private String resetPasswordToken;
    private Instant resetPasswordExpires;

    // Feature 8: availability schedule
    private boolean scheduleEnabled = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_schedule_days", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "day")
    private List<String> scheduleDays = new ArrayList<>();

    private String scheduleStartTime = "09:00";
    private String scheduleEndTime = "18:00";

    // Feature 7: rate limiting
    private int openRequestCount = 0;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getResetPasswordToken() { return resetPasswordToken; }
    public void setResetPasswordToken(String t) { this.resetPasswordToken = t; }
    public Instant getResetPasswordExpires() { return resetPasswordExpires; }
    public void setResetPasswordExpires(Instant e) { this.resetPasswordExpires = e; }
    public boolean isScheduleEnabled() { return scheduleEnabled; }
    public void setScheduleEnabled(boolean v) { this.scheduleEnabled = v; }
    public List<String> getScheduleDays() { return scheduleDays; }
    public void setScheduleDays(List<String> d) { this.scheduleDays = d; }
    public String getScheduleStartTime() { return scheduleStartTime; }
    public void setScheduleStartTime(String s) { this.scheduleStartTime = s; }
    public String getScheduleEndTime() { return scheduleEndTime; }
    public void setScheduleEndTime(String s) { this.scheduleEndTime = s; }
    public int getOpenRequestCount() { return openRequestCount; }
    public void setOpenRequestCount(int c) { this.openRequestCount = c; }
}
