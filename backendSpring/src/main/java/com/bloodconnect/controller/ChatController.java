package com.bloodconnect.controller;

import com.bloodconnect.model.BloodRequest;
import com.bloodconnect.model.ChatMessage;
import com.bloodconnect.model.Donation;
import com.bloodconnect.model.User;
import com.bloodconnect.repository.BloodRequestRepository;
import com.bloodconnect.repository.ChatMessageRepository;
import com.bloodconnect.repository.DonationRepository;
import com.bloodconnect.repository.UserRepository;
import com.bloodconnect.service.SocketService;
import com.bloodconnect.util.ApiException;
import com.bloodconnect.util.Presenter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatMessageRepository chatRepo;
    private final DonationRepository donationRepo;
    private final BloodRequestRepository requestRepo;
    private final UserRepository userRepo;
    private final SocketService socket;
    private final Presenter present;

    public ChatController(ChatMessageRepository chatRepo, DonationRepository donationRepo,
                          BloodRequestRepository requestRepo, UserRepository userRepo,
                          SocketService socket, Presenter present) {
        this.chatRepo = chatRepo; this.donationRepo = donationRepo; this.requestRepo = requestRepo;
        this.userRepo = userRepo; this.socket = socket; this.present = present;
    }

    private void requireAuth(User u) { if (u == null) throw new ApiException(401, "No token provided"); }

    /** Returns [donation, request] if the user is the donor or requester, else null. */
    private Object[] verifyAccess(String donationId, String userId) {
        Donation d = donationRepo.findById(donationId).orElse(null);
        if (d == null) return null;
        BloodRequest r = requestRepo.findById(d.getRequestId()).orElse(null);
        if (r == null) return null;
        boolean isDonor = d.getDonorId().equals(userId);
        boolean isRequester = r.getRequesterId().equals(userId);
        return (isDonor || isRequester) ? new Object[]{d, r} : null;
    }

    @GetMapping("/{donationId}")
    public Map<String, Object> getMessages(@AuthenticationPrincipal User user, @PathVariable String donationId) {
        requireAuth(user);
        Object[] access = verifyAccess(donationId, user.getId());
        if (access == null) throw new ApiException(403, "Not authorized");
        Donation d = (Donation) access[0];
        BloodRequest r = (BloodRequest) access[1];

        List<ChatMessage> msgs = chatRepo.findByDonationIdOrderByCreatedAtAsc(donationId);
        if (msgs.size() > 100) msgs = msgs.subList(0, 100);

        // mark received unread as read
        List<ChatMessage> unread = chatRepo.findByDonationIdAndReceiverIdAndReadFalse(donationId, user.getId());
        unread.forEach(m -> m.setRead(true));
        chatRepo.saveAll(unread);

        List<Map<String, Object>> out = new ArrayList<>();
        for (ChatMessage m : msgs) {
            User sender = userRepo.findById(m.getSenderId()).orElse(null);
            out.add(present.chatMessage(m, sender));
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("messages", out);
        res.put("donation", present.donation(d, null, present.requestBrief(r, "bloodType", "hospital")));
        return res;
    }

    @PostMapping("/{donationId}")
    public Map<String, Object> postMessage(@AuthenticationPrincipal User user, @PathVariable String donationId,
                                           @RequestBody Map<String, Object> b) {
        requireAuth(user);
        String message = b.get("message") == null ? null : b.get("message").toString();
        if (message == null || message.trim().isEmpty()) throw new ApiException(400, "Empty message");

        Object[] access = verifyAccess(donationId, user.getId());
        if (access == null) throw new ApiException(403, "Not authorized");
        Donation d = (Donation) access[0];
        BloodRequest r = (BloodRequest) access[1];

        String receiverId = d.getDonorId().equals(user.getId()) ? r.getRequesterId() : d.getDonorId();

        ChatMessage m = new ChatMessage();
        m.setDonationId(donationId);
        m.setSenderId(user.getId());
        m.setReceiverId(receiverId);
        m.setMessage(message.trim().substring(0, Math.min(500, message.trim().length())));
        m = chatRepo.save(m);

        Map<String, Object> populated = present.chatMessage(m, user);

        socket.toChat(donationId, "chat_message", populated);
        Map<String, Object> notif = new LinkedHashMap<>();
        notif.put("donationId", donationId);
        notif.put("from", user.getName());
        notif.put("preview", message.substring(0, Math.min(60, message.length())));
        socket.toUser(receiverId, "chat_notification", notif);

        return populated;
    }

    @GetMapping("/{donationId}/unread")
    public Map<String, Object> unread(@AuthenticationPrincipal User user, @PathVariable String donationId) {
        requireAuth(user);
        long count = chatRepo.countByDonationIdAndReceiverIdAndReadFalse(donationId, user.getId());
        return Map.of("count", count);
    }
}
