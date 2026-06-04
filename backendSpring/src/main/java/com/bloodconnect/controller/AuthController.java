package com.bloodconnect.controller;

import com.bloodconnect.model.DonorProfile;
import com.bloodconnect.model.User;
import com.bloodconnect.repository.DonorProfileRepository;
import com.bloodconnect.repository.UserRepository;
import com.bloodconnect.security.JwtUtil;
import com.bloodconnect.service.EmailService;
import com.bloodconnect.util.ApiException;
import com.bloodconnect.util.Presenter;
import com.bloodconnect.util.Tokens;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final DonorProfileRepository donorRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;
    private final Presenter present;
    private final EmailService email;
    private final String adminSecret;
    private final String clientUrl;

    public AuthController(UserRepository userRepo, DonorProfileRepository donorRepo,
                          PasswordEncoder encoder, JwtUtil jwt, Presenter present, EmailService email,
                          @Value("${app.admin.secret}") String adminSecret,
                          @Value("${app.client.url}") String clientUrl) {
        this.userRepo = userRepo; this.donorRepo = donorRepo; this.encoder = encoder;
        this.jwt = jwt; this.present = present; this.email = email;
        this.adminSecret = adminSecret; this.clientUrl = clientUrl;
    }

    private static String str(Map<String, Object> b, String k) {
        Object v = b.get(k); return v == null ? null : v.toString();
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, Object> b) {
        String name = str(b, "name"), em = str(b, "email"), password = str(b, "password");
        String phone = str(b, "phone"), role = str(b, "role"), bloodType = str(b, "bloodType");
        String city = str(b, "city"), adminSecretIn = str(b, "adminSecret");

        if (name == null || em == null || password == null)
            throw new ApiException(400, "All fields required");
        if (userRepo.existsByEmailIgnoreCase(em))
            throw new ApiException(409, "Email already registered");
        if ("admin".equals(role) && !adminSecret.equals(adminSecretIn))
            throw new ApiException(403, "Invalid admin secret code");

        User u = new User();
        u.setName(name);
        u.setEmail(em.toLowerCase());
        u.setPassword(encoder.encode(password));
        u.setPhone(phone == null ? "" : phone);
        u.setRole(role == null ? "donor" : role);
        u = userRepo.save(u);

        if ("donor".equals(role) && bloodType != null) {
            DonorProfile dp = new DonorProfile();
            dp.setUserId(u.getId());
            dp.setBloodType(bloodType);
            dp.setCity(city == null ? "" : city);
            donorRepo.save(dp);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("token", jwt.sign(u.getId()));
        res.put("user", present.userAuth(u));
        return res;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> b) {
        String em = str(b, "email"), password = str(b, "password");
        User u = userRepo.findByEmailIgnoreCase(em == null ? "" : em).orElse(null);
        if (u == null || !encoder.matches(password == null ? "" : password, u.getPassword()))
            throw new ApiException(401, "Invalid credentials");

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("token", jwt.sign(u.getId()));
        res.put("user", present.userAuth(u));
        res.put("donorProfile", "donor".equals(u.getRole())
            ? donorRepo.findByUserId(u.getId()).map(p -> present.donorProfile(p, null)).orElse(null)
            : null);
        return res;
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal User user) {
        if (user == null) throw new ApiException(401, "No token provided");
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("user", present.userFull(user));
        res.put("donorProfile", "donor".equals(user.getRole())
            ? donorRepo.findByUserId(user.getId()).map(p -> present.donorProfile(p, null)).orElse(null)
            : null);
        return res;
    }

    @PostMapping("/forgot-password")
    public Map<String, Object> forgotPassword(@RequestBody Map<String, Object> b) {
        String em = str(b, "email");
        if (em == null) throw new ApiException(400, "Email required");
        String safe = "If that email exists, a reset link has been sent.";
        User u = userRepo.findByEmailIgnoreCase(em.toLowerCase()).orElse(null);
        if (u == null) return Map.of("message", safe);

        String raw = Tokens.randomToken();
        u.setResetPasswordToken(Tokens.sha256Hex(raw));
        u.setResetPasswordExpires(Instant.now().plusSeconds(30 * 60));
        userRepo.save(u);

        String url = clientUrl + "/reset-password/" + raw;
        String html = "<div style=\"font-family:sans-serif;max-width:480px;margin:0 auto\">"
            + "<h2 style=\"color:#e53e3e\">\uD83E\uDE78 BloodConnect</h2><h3>Reset your password</h3>"
            + "<p>Hi " + u.getName() + ",</p>"
            + "<p>Click below to reset your password. This link expires in <strong>30 minutes</strong>.</p>"
            + "<a href=\"" + url + "\" style=\"display:inline-block;margin:16px 0;background:#e53e3e;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none;font-weight:600\">Reset Password</a>"
            + "<p style=\"color:#888;font-size:12px\">If you didn't request this, ignore this email.</p></div>";
        email.sendHtml(u.getEmail(), "BloodConnect \u2014 Password Reset", html);
        return Map.of("message", safe);
    }

    @PostMapping("/reset-password/{token}")
    public Map<String, Object> resetPassword(@PathVariable String token, @RequestBody Map<String, Object> b) {
        String password = str(b, "password");
        if (password == null || password.length() < 6)
            throw new ApiException(400, "Password must be at least 6 characters");

        String hashed = Tokens.sha256Hex(token);
        User u = userRepo.findByResetPasswordToken(hashed).orElse(null);
        if (u == null || u.getResetPasswordExpires() == null || u.getResetPasswordExpires().isBefore(Instant.now()))
            throw new ApiException(400, "Token is invalid or has expired");

        u.setPassword(encoder.encode(password));
        u.setResetPasswordToken(null);
        u.setResetPasswordExpires(null);
        userRepo.save(u);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("message", "Password reset successful");
        res.put("token", jwt.sign(u.getId()));
        return res;
    }

    @PutMapping("/availability-schedule")
    public Map<String, Object> availabilitySchedule(@AuthenticationPrincipal User user,
                                                    @RequestBody Map<String, Object> b) {
        if (user == null) throw new ApiException(401, "No token provided");
        boolean enabled = Boolean.TRUE.equals(b.get("enabled"));
        @SuppressWarnings("unchecked")
        List<String> days = b.get("days") instanceof List ? (List<String>) b.get("days") : new ArrayList<>();
        String startTime = str(b, "startTime");
        String endTime = str(b, "endTime");

        user.setScheduleEnabled(enabled);
        user.setScheduleDays(days);
        if (startTime != null) user.setScheduleStartTime(startTime);
        if (endTime != null) user.setScheduleEndTime(endTime);
        userRepo.save(user);

        if ("donor".equals(user.getRole())) {
            java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
            String[] dayNames = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
            String today = dayNames[now.getDayOfWeek().getValue() % 7];
            String current = String.format("%02d:%02d", now.getHour(), now.getMinute());
            String s = startTime == null ? "09:00" : startTime;
            String e = endTime == null ? "18:00" : endTime;
            boolean available = !enabled || (days.contains(today)
                && current.compareTo(s) >= 0 && current.compareTo(e) <= 0);
            donorRepo.findByUserId(user.getId()).ifPresent(dp -> {
                dp.setAvailable(available);
                donorRepo.save(dp);
            });
        }
        return Map.of("message", "Schedule updated");
    }
}
