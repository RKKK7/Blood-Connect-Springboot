package com.bloodconnect.controller;

import com.bloodconnect.model.Notification;
import com.bloodconnect.model.User;
import com.bloodconnect.repository.NotificationRepository;
import com.bloodconnect.util.ApiException;
import com.bloodconnect.util.Presenter;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notifRepo;
    private final Presenter present;

    public NotificationController(NotificationRepository notifRepo, Presenter present) {
        this.notifRepo = notifRepo; this.present = present;
    }

    private void requireAuth(User u) { if (u == null) throw new ApiException(401, "No token provided"); }

    @GetMapping
    public Map<String, Object> list(@AuthenticationPrincipal User user) {
        requireAuth(user);
        List<Notification> notifs = notifRepo.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 30));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Notification n : notifs) out.add(present.notification(n));
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("notifications", out);
        res.put("unreadCount", notifRepo.countByUserIdAndReadFalse(user.getId()));
        return res;
    }

    @PutMapping("/read-all")
    public Map<String, Object> readAll(@AuthenticationPrincipal User user) {
        requireAuth(user);
        List<Notification> unread = notifRepo.findByUserIdAndReadFalse(user.getId());
        unread.forEach(n -> n.setRead(true));
        notifRepo.saveAll(unread);
        return Map.of("message", "All marked as read");
    }

    @PutMapping("/{id}/read")
    public Map<String, Object> read(@AuthenticationPrincipal User user, @PathVariable String id) {
        requireAuth(user);
        notifRepo.findById(id).ifPresent(n -> { n.setRead(true); notifRepo.save(n); });
        return Map.of("message", "Marked as read");
    }
}
