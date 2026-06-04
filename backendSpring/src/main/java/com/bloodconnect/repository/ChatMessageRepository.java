package com.bloodconnect.repository;

import com.bloodconnect.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    List<ChatMessage> findByDonationIdOrderByCreatedAtAsc(String donationId);
    List<ChatMessage> findByDonationIdAndReceiverIdAndReadFalse(String donationId, String receiverId);
    long countByDonationIdAndReceiverIdAndReadFalse(String donationId, String receiverId);
}
