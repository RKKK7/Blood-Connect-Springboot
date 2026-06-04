package com.bloodconnect.controller;

import com.bloodconnect.model.BloodRequest;
import com.bloodconnect.model.DonorProfile;
import com.bloodconnect.repository.BloodRequestRepository;
import com.bloodconnect.repository.DonorProfileRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * Feature 6: Blood Shortage Dashboard (public).
 * Per-city, per-blood-type availability level: red / yellow / green.
 */
@RestController
@RequestMapping("/api/shortage")
public class ShortageController {

    private static final String[] BLOOD_TYPES = {"A+","A-","B+","B-","AB+","AB-","O+","O-"};

    private final DonorProfileRepository donorRepo;
    private final BloodRequestRepository requestRepo;

    public ShortageController(DonorProfileRepository donorRepo, BloodRequestRepository requestRepo) {
        this.donorRepo = donorRepo; this.requestRepo = requestRepo;
    }

    @GetMapping
    public List<Map<String, Object>> shortage() {
        // city -> bloodType -> [donors, requests]
        Map<String, Map<String, int[]>> cityMap = new HashMap<>();

        for (DonorProfile d : donorRepo.findByAvailableTrue()) {
            String city = d.getCity();
            if (city == null || city.isBlank()) continue;
            cityMap.computeIfAbsent(city, k -> new HashMap<>())
                .computeIfAbsent(d.getBloodType(), k -> new int[2])[0]++;
        }
        for (BloodRequest r : requestRepo.findAll()) {
            if (!"open".equals(r.getStatus())) continue;
            String city = r.getCity();
            if (city == null || city.isBlank()) continue;
            cityMap.computeIfAbsent(city, k -> new HashMap<>())
                .computeIfAbsent(r.getBloodType(), k -> new int[2])[1]++;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, int[]>> e : cityMap.entrySet()) {
            Map<String, Object> bloodTypes = new LinkedHashMap<>();
            for (String bt : BLOOD_TYPES) {
                int[] dr = e.getValue().getOrDefault(bt, new int[2]);
                int donors = dr[0], requests = dr[1];
                String level = "green";
                if (requests > 0 && donors == 0) level = "red";
                else if (requests > 0 && donors <= requests) level = "yellow";
                Map<String, Object> cell = new LinkedHashMap<>();
                cell.put("donors", donors);
                cell.put("requests", requests);
                cell.put("level", level);
                bloodTypes.put(bt, cell);
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("city", e.getKey());
            row.put("bloodTypes", bloodTypes);
            result.add(row);
        }
        result.sort(Comparator.comparing(m -> (String) m.get("city")));
        return result;
    }
}
