package com.bloodconnect.service;

import com.bloodconnect.model.Notification;
import com.bloodconnect.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SocketService socketService;
    private final EmailService emailService;
    private final String clientUrl;

    public NotificationService(NotificationRepository notificationRepository,
                               SocketService socketService,
                               EmailService emailService,
                               @Value("${app.client.url}") String clientUrl) {
        this.notificationRepository = notificationRepository;
        this.socketService = socketService;
        this.emailService = emailService;
        this.clientUrl = clientUrl;
    }

    public Notification send(String userId, String title, String message, String type, String link, String email) {
        try {
            Notification n = new Notification();
            n.setUserId(userId);
            n.setTitle(title);
            n.setMessage(message);
            n.setType(type == null ? "system" : type);
            n.setLink(link == null ? "" : link);
            n = notificationRepository.save(n);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("title", title);
            payload.put("message", message);
            payload.put("type", n.getType());
            payload.put("link", n.getLink());
            socketService.toUser(userId, "notification", payload);

            if (email != null && !email.isBlank()) {
                String html = "<div style=\"font-family:sans-serif;max-width:500px;margin:0 auto\">"
                    + "<h2 style=\"color:#e53e3e\">\uD83E\uDE78 BloodConnect</h2>"
                    + "<h3>" + title + "</h3><p>" + message + "</p>"
                    + (link != null && !link.isBlank()
                        ? "<a href=\"" + clientUrl + link + "\" style=\"background:#e53e3e;color:#fff;padding:10px 20px;border-radius:6px;text-decoration:none;display:inline-block;margin-top:12px\">View Details</a>"
                        : "")
                    + "<hr style=\"margin-top:24px\"/>"
                    + "<p style=\"color:#888;font-size:12px\">BloodConnect — saving lives together</p></div>";
                emailService.sendHtml(email, "BloodConnect: " + title, html);
            }
            return n;
        } catch (Exception e) {
            System.err.println("Notification error: " + e.getMessage());
            return null;
        }
    }

    public void send(String userId, String title, String message, String type, String link) {
        send(userId, title, message, type, link, null);
    }

    /** io.emit("new_request", request) */
    public void broadcastRequest(Object requestPayload) {
        socketService.broadcast("new_request", requestPayload);
    }
}
