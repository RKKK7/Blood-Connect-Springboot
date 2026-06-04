package com.bloodconnect.model;

import jakarta.persistence.*;

@Entity
@Table(name = "chat_messages")
public class ChatMessage extends BaseEntity {

    @Column(nullable = false)
    private String donationId;

    @Column(nullable = false)
    private String senderId;

    @Column(nullable = false)
    private String receiverId;

    @Column(nullable = false, length = 500)
    private String message;

    private boolean read = false;

    public String getDonationId() { return donationId; }
    public void setDonationId(String d) { this.donationId = d; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String s) { this.senderId = s; }
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String r) { this.receiverId = r; }
    public String getMessage() { return message; }
    public void setMessage(String m) { this.message = m; }
    public boolean isRead() { return read; }
    public void setRead(boolean v) { this.read = v; }
}
