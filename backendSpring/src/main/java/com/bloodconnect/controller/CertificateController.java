package com.bloodconnect.controller;

import com.bloodconnect.model.BloodRequest;
import com.bloodconnect.model.Donation;
import com.bloodconnect.model.DonorProfile;
import com.bloodconnect.model.User;
import com.bloodconnect.repository.BloodRequestRepository;
import com.bloodconnect.repository.DonationRepository;
import com.bloodconnect.repository.DonorProfileRepository;
import com.bloodconnect.repository.UserRepository;
import com.bloodconnect.security.JwtUtil;
import com.bloodconnect.util.ApiException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@RestController
@RequestMapping("/api/certificate")
public class CertificateController {

    private final DonationRepository donationRepo;
    private final BloodRequestRepository requestRepo;
    private final DonorProfileRepository donorRepo;
    private final UserRepository userRepo;
    private final JwtUtil jwt;

    public CertificateController(DonationRepository donationRepo, BloodRequestRepository requestRepo,
                                 DonorProfileRepository donorRepo, UserRepository userRepo, JwtUtil jwt) {
        this.donationRepo = donationRepo; this.requestRepo = requestRepo;
        this.donorRepo = donorRepo; this.userRepo = userRepo; this.jwt = jwt;
    }

    // GET /api/certificate/:donationId?token=<jwt>
    @GetMapping(value = "/{donationId}", produces = MediaType.TEXT_HTML_VALUE)
    public String certificate(@PathVariable String donationId,
                              @RequestParam(required = false) String token) {
        if (token == null || token.isBlank())
            throw new ApiException(401, "No token provided. Add ?token=YOUR_JWT to the URL.");
        User currentUser;
        try {
            String uid = jwt.parseUserId(token);
            currentUser = userRepo.findById(uid).orElseThrow();
        } catch (Exception e) {
            throw new ApiException(401, "Invalid or expired token");
        }

        Donation donation = donationRepo.findById(donationId)
            .orElseThrow(() -> new ApiException(404, "Donation not found"));
        if (!"completed".equals(donation.getStatus()))
            throw new ApiException(400, "Certificate only available for completed donations");

        User donor = userRepo.findById(donation.getDonorId()).orElse(null);
        if (donor == null || (!donor.getId().equals(currentUser.getId()) && !"admin".equals(currentUser.getRole())))
            throw new ApiException(403, "Not authorized");

        DonorProfile profile = donorRepo.findByUserId(donation.getDonorId()).orElse(null);
        BloodRequest request = requestRepo.findById(donation.getRequestId()).orElse(null);

        String donorName = donor.getName();
        Instant when = donation.getDonatedAt() != null ? donation.getDonatedAt() : Instant.now();
        String donatedDate = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
            .withZone(ZoneId.of("Asia/Kolkata")).format(when);
        String certId = "BC-" + donationId.replace("-", "")
            .substring(Math.max(0, donationId.replace("-", "").length() - 8)).toUpperCase();

        String bloodType = request != null ? request.getBloodType()
            : (profile != null ? profile.getBloodType() : "blood");
        String hospital = request != null ? request.getHospital() : "the hospital";
        String city = request != null ? request.getCity() : "";
        String patientName = request != null ? request.getPatientName() : "a patient in need";
        int totalDonations = profile != null ? profile.getTotalDonations() : 1;

        StringBuilder badgeRow = new StringBuilder();
        if (profile != null && profile.getBadges() != null && !profile.getBadges().isEmpty()) {
            badgeRow.append("<div class=\"badge-row\">");
            for (String b : profile.getBadges())
                badgeRow.append("<span class=\"badge\">").append(b).append("</span>");
            badgeRow.append("</div>");
        }

        return CERT_TEMPLATE
            .replace("%%DONOR_NAME%%", donorName)
            .replace("%%BLOOD_TYPE%%", bloodType == null ? "\u2014" : bloodType)
            .replace("%%DONATED_DATE%%", donatedDate)
            .replace("%%HOSPITAL%%", hospital == null ? "\u2014" : hospital)
            .replace("%%CITY%%", city == null ? "" : city)
            .replace("%%PATIENT_NAME%%", patientName)
            .replace("%%TOTAL_DONATIONS%%", String.valueOf(totalDonations))
            .replace("%%BADGE_ROW%%", badgeRow.toString())
            .replace("%%CERT_ID%%", certId);
    }

    private static final String CERT_TEMPLATE = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <title>BloodConnect — Donation Certificate</title>
  <style>
    @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@700&family=Inter:wght@400;500;600&display=swap');
    * { margin:0; padding:0; box-sizing:border-box; }
    body { font-family:'Inter',sans-serif; background:#f9f9f9; display:flex; justify-content:center; align-items:center; min-height:100vh; padding:40px 20px; }
    .cert { background:#fff; width:720px; padding:60px; border:3px solid #e53e3e; border-radius:16px; box-shadow:0 20px 60px rgba(0,0,0,0.1); position:relative; overflow:hidden; }
    .cert::before { content:''; position:absolute; top:10px; left:10px; right:10px; bottom:10px; border:1px solid rgba(229,62,62,0.2); border-radius:12px; pointer-events:none; }
    .watermark { position:absolute; top:50%; left:50%; transform:translate(-50%,-50%) rotate(-30deg); font-size:120px; color:rgba(229,62,62,0.04); font-weight:700; white-space:nowrap; pointer-events:none; }
    .logo { display:flex; align-items:center; gap:10px; margin-bottom:32px; }
    .logo-icon { width:44px; height:44px; background:#e53e3e; border-radius:10px; display:flex; align-items:center; justify-content:center; font-size:22px; }
    .logo-text { font-family:'Playfair Display',serif; font-size:22px; color:#e53e3e; font-weight:700; }
    .title { text-align:center; margin-bottom:28px; }
    .title h1 { font-family:'Playfair Display',serif; font-size:36px; color:#1a1a1a; margin-bottom:8px; }
    .title p { color:#888; font-size:14px; letter-spacing:2px; text-transform:uppercase; }
    .divider { border:none; border-top:2px solid #e53e3e; width:80px; display:block; margin:16px auto 28px; }
    .body-text { text-align:center; color:#555; font-size:15px; line-height:1.8; margin-bottom:32px; }
    .donor-name { font-family:'Playfair Display',serif; font-size:36px; color:#e53e3e; text-align:center; margin:24px 0; padding:16px; border-top:1px dashed #e5e5e5; border-bottom:1px dashed #e5e5e5; }
    .details { display:grid; grid-template-columns:1fr 1fr; gap:16px; margin:32px 0; }
    .detail-box { background:#fff5f5; border:1px solid rgba(229,62,62,0.15); border-radius:10px; padding:14px 18px; }
    .detail-label { font-size:11px; color:#999; font-weight:600; text-transform:uppercase; letter-spacing:1px; margin-bottom:4px; }
    .detail-value { font-size:15px; color:#1a1a1a; font-weight:600; }
    .footer { display:flex; justify-content:space-between; align-items:flex-end; margin-top:40px; border-top:1px solid #e5e5e5; padding-top:24px; }
    .cert-id { font-size:11px; color:#bbb; letter-spacing:1px; }
    .sig-line { border-top:1px solid #ccc; width:160px; margin:0 auto 6px; }
    .sig-text { font-size:12px; color:#888; text-align:center; }
    .badge-row { display:flex; justify-content:center; gap:10px; margin:20px 0; flex-wrap:wrap; }
    .badge { background:#fff5f5; border:1px solid rgba(229,62,62,0.2); border-radius:100px; padding:4px 14px; font-size:12px; color:#e53e3e; font-weight:600; }
    .print-btn { position:fixed; bottom:24px; right:24px; background:#e53e3e; color:#fff; border:none; padding:12px 24px; border-radius:10px; font-size:15px; font-weight:600; cursor:pointer; box-shadow:0 4px 16px rgba(229,62,62,0.4); }
    @media print { body { background:white; } .cert { box-shadow:none; } .print-btn { display:none; } }
  </style>
</head>
<body>
  <div class="cert">
    <div class="watermark">🩸</div>
    <div class="logo">
      <div class="logo-icon">🩸</div>
      <span class="logo-text">BloodConnect</span>
    </div>
    <div class="title">
      <h1>Certificate of Donation</h1>
      <p>Blood Donation Achievement</p>
    </div>
    <hr class="divider" />
    <p class="body-text">This is to certify that</p>
    <div class="donor-name">%%DONOR_NAME%%</div>
    <p class="body-text">
      has heroically donated <strong>%%BLOOD_TYPE%%</strong>
      blood on <strong>%%DONATED_DATE%%</strong> at <strong>%%HOSPITAL%%</strong>,
      <strong>%%CITY%%</strong>, contributing to the care of
      <strong>%%PATIENT_NAME%%</strong>.<br /><br />
      This selfless act can save up to <strong>3 lives</strong>.
    </p>
    %%BADGE_ROW%%
    <div class="details">
      <div class="detail-box">
        <p class="detail-label">Blood Type Donated</p>
        <p class="detail-value">%%BLOOD_TYPE%%</p>
      </div>
      <div class="detail-box">
        <p class="detail-label">Date of Donation</p>
        <p class="detail-value">%%DONATED_DATE%%</p>
      </div>
      <div class="detail-box">
        <p class="detail-label">Hospital</p>
        <p class="detail-value">%%HOSPITAL%%</p>
      </div>
      <div class="detail-box">
        <p class="detail-label">Total Donations</p>
        <p class="detail-value">%%TOTAL_DONATIONS%%</p>
      </div>
    </div>
    <div class="footer">
      <div class="cert-id">Certificate ID: %%CERT_ID%%</div>
      <div>
        <div class="sig-line"></div>
        <p class="sig-text">BloodConnect Platform</p>
        <p class="sig-text" style="color:#bbb;font-size:10px;">AI-Powered Blood Donation Network</p>
      </div>
    </div>
  </div>
  <button class="print-btn" onclick="window.print()">🖨️ Print / Save as PDF</button>
</body>
</html>""";
}
