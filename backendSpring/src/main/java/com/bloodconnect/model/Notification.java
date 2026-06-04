package com.bloodconnect.model;

import jakarta.persistence.*;

@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    private String type = "system"; // match | request | donation | system | alert
    private boolean read = false;
    private String link = "";

    public String getUserId() { return userId; }
    public void setUserId(String u) { this.userId = u; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getMessage() { return message; }
    public void setMessage(String m) { this.message = m; }
    public String getType() { return type; }
    public void setType(String t) { this.type = t; }
    public boolean isRead() { return read; }
    public void setRead(boolean v) { this.read = v; }
    public String getLink() { return link; }
    public void setLink(String l) { this.link = l; }
}
