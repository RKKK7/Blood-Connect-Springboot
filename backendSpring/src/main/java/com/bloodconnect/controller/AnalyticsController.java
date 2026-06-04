package com.bloodconnect.controller;

import com.bloodconnect.model.BloodRequest;
import com.bloodconnect.model.Donation;
import com.bloodconnect.model.User;
import com.bloodconnect.repository.*;
import com.bloodconnect.service.AiService;
import com.bloodconnect.util.Presenter;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final BloodRequestRepository requestRepo;
    private final DonationRepository donationRepo;
    private final DonorProfileRepository donorRepo;
    private final UserRepository userRepo;
    private final AiService ai;
    private final Presenter present;

    public AnalyticsController(BloodRequestRepository requestRepo, DonationRepository donationRepo,
                               DonorProfileRepository donorRepo, UserRepository userRepo,
                               AiService ai, Presenter present) {
        this.requestRepo = requestRepo; this.donationRepo = donationRepo; this.donorRepo = donorRepo;
        this.userRepo = userRepo; this.ai = ai; this.present = present;
    }

    // GET /api/analytics/public
    @GetMapping("/public")
    public Map<String, Object> publicStats() {
        long totalDonations = donationRepo.countByStatus("completed");
        long totalRequests = requestRepo.count();
        long fulfilledRequests = requestRepo.countByStatus("fulfilled");
        long totalDonors = donorRepo.count();
        long activeCities = requestRepo.findAll().stream()
            .map(BloodRequest::getCity).filter(c -> c != null && !c.isBlank())
            .distinct().count();

        List<Donation> recent = donationRepo.findByStatusOrderByDonatedAtDesc("completed")
            .stream().limit(5).toList();
        List<Map<String, Object>> recentOut = new ArrayList<>();
        for (Donation d : recent) {
            User donor = userRepo.findById(d.getDonorId()).orElse(null);
            BloodRequest req = requestRepo.findById(d.getRequestId()).orElse(null);
            recentOut.add(present.donation(d,
                donor != null ? present.userBrief(donor, "name") : null,
                present.requestBrief(req, "bloodType", "hospital", "city", "patientName")));
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("totalDonations", totalDonations);
        res.put("totalRequests", totalRequests);
        res.put("fulfilledRequests", fulfilledRequests);
        res.put("totalDonors", totalDonors);
        res.put("activeCities", activeCities);
        res.put("livesClaimed", totalDonations * 3);
        res.put("fulfillmentRate", totalRequests > 0 ? Math.round((double) fulfilledRequests / totalRequests * 100) : 0);
        res.put("recentDonations", recentOut);
        return res;
    }

    // GET /api/analytics/admin
    @GetMapping("/admin")
    public Map<String, Object> adminStats() {
        Instant thirtyDaysAgo = Instant.now().minus(Duration.ofDays(30));
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
        List<BloodRequest> all = requestRepo.findAll();

        // dailyRequests (last 30 days)
        Map<String, int[]> daily = new TreeMap<>(); // [total, critical, fulfilled]
        for (BloodRequest r : all) {
            if (r.getCreatedAt() == null || r.getCreatedAt().isBefore(thirtyDaysAgo)) continue;
            String day = dayFmt.format(r.getCreatedAt());
            int[] c = daily.computeIfAbsent(day, k -> new int[3]);
            c[0]++;
            if ("critical".equals(r.getUrgency())) c[1]++;
            if ("fulfilled".equals(r.getStatus())) c[2]++;
        }
        List<Map<String, Object>> dailyRequests = new ArrayList<>();
        daily.forEach((day, c) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("_id", day); m.put("total", c[0]); m.put("critical", c[1]); m.put("fulfilled", c[2]);
            dailyRequests.add(m);
        });

        // byBloodType
        Map<String, int[]> bt = new HashMap<>();
        for (BloodRequest r : all) {
            int[] c = bt.computeIfAbsent(r.getBloodType(), k -> new int[3]);
            c[0]++;
            if ("fulfilled".equals(r.getStatus())) c[1]++;
            if ("critical".equals(r.getUrgency())) c[2]++;
        }
        List<Map<String, Object>> byBloodType = bt.entrySet().stream()
            .sorted((a, b) -> b.getValue()[0] - a.getValue()[0])
            .map(e -> { Map<String, Object> m = new LinkedHashMap<>();
                m.put("_id", e.getKey()); m.put("total", e.getValue()[0]);
                m.put("fulfilled", e.getValue()[1]); m.put("critical", e.getValue()[2]); return m; })
            .collect(Collectors.toList());

        // avgResponseHours
        Map<String, Instant> reqCreated = all.stream()
            .collect(Collectors.toMap(BloodRequest::getId, BloodRequest::getCreatedAt, (a, b) -> a));
        List<Long> hours = new ArrayList<>();
        for (Donation d : donationRepo.findAll()) {
            if ("cancelled".equals(d.getStatus())) continue;
            Instant rc = reqCreated.get(d.getRequestId());
            if (rc == null || d.getCreatedAt() == null) continue;
            long h = Duration.between(rc, d.getCreatedAt()).toHours();
            if (h >= 0 && h < 720) hours.add(h);
        }
        long avgResponseHours = hours.isEmpty() ? 0
            : Math.round(hours.stream().mapToLong(Long::longValue).average().orElse(0));

        // topCities
        Map<String, Long> cityCounts = all.stream()
            .collect(Collectors.groupingBy(BloodRequest::getCity, Collectors.counting()));
        List<Map<String, Object>> topCities = cityCounts.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).limit(8)
            .map(e -> { Map<String, Object> m = new LinkedHashMap<>();
                m.put("_id", e.getKey()); m.put("count", e.getValue()); return m; })
            .collect(Collectors.toList());

        // weeklyUsers (last 4 weeks)
        Instant fourWeeksAgo = Instant.now().minus(Duration.ofDays(28));
        WeekFields wf = WeekFields.ISO;
        Map<Integer, Long> weekly = new TreeMap<>();
        for (User u : userRepo.findAll()) {
            if (u.getCreatedAt() == null || u.getCreatedAt().isBefore(fourWeeksAgo)) continue;
            int week = u.getCreatedAt().atZone(ZoneOffset.UTC).get(wf.weekOfWeekBasedYear());
            weekly.merge(week, 1L, Long::sum);
        }
        List<Map<String, Object>> weeklyUsers = weekly.entrySet().stream()
            .map(e -> { Map<String, Object> m = new LinkedHashMap<>();
                m.put("_id", e.getKey()); m.put("count", e.getValue()); return m; })
            .collect(Collectors.toList());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("dailyRequests", dailyRequests);
        res.put("byBloodType", byBloodType);
        res.put("avgResponseHours", avgResponseHours);
        res.put("topCities", topCities);
        res.put("weeklyUsers", weeklyUsers);
        return res;
    }

    // GET /api/analytics/forecast
    @GetMapping("/forecast")
    public Map<String, Object> forecast() {
        Instant thirtyDaysAgo = Instant.now().minus(Duration.ofDays(30));
        Map<String, int[]> grouped = new HashMap<>(); // key bloodType|city -> [count, critical, fulfilled]
        Map<String, String[]> labels = new HashMap<>();
        for (BloodRequest r : requestRepo.findAll()) {
            if (r.getCreatedAt() == null || r.getCreatedAt().isBefore(thirtyDaysAgo)) continue;
            String key = r.getBloodType() + "|" + r.getCity();
            int[] c = grouped.computeIfAbsent(key, k -> new int[3]);
            c[0]++;
            if ("critical".equals(r.getUrgency())) c[1]++;
            if ("fulfilled".equals(r.getStatus())) c[2]++;
            labels.putIfAbsent(key, new String[]{r.getBloodType(), r.getCity()});
        }
        List<Map<String, Object>> historical = grouped.entrySet().stream()
            .sorted((a, b) -> b.getValue()[0] - a.getValue()[0]).limit(20)
            .map(e -> { Map<String, Object> m = new LinkedHashMap<>();
                m.put("bloodType", labels.get(e.getKey())[0]);
                m.put("city", labels.get(e.getKey())[1]);
                m.put("count", e.getValue()[0]);
                m.put("critical", e.getValue()[1]);
                m.put("fulfilled", e.getValue()[2]); return m; })
            .collect(Collectors.toList());
        return ai.forecastDemand(historical);
    }
}
