package com.bloodconnect.service;

import com.bloodconnect.model.BloodRequest;
import com.bloodconnect.model.DonorProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Groq integration. Mirrors services/aiService.js (model llama-3.1-8b-instant,
 * JSON-only prompts, temperature 0.3). Falls back to safe defaults on any error.
 */
@Service
public class AiService {

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final RestClient http = RestClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    public AiService(@Value("${app.groq.api-key}") String apiKey,
                     @Value("${app.groq.model}") String model,
                     @Value("${app.groq.base-url}") String baseUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
    }

    private String chat(String prompt, String systemPrompt) {
        Map<String, Object> body = Map.of(
            "model", model,
            "temperature", 0.3,
            "max_tokens", 500,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", prompt)
            )
        );
        JsonNode res = http.post()
            .uri(baseUrl)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .body(body)
            .retrieve()
            .body(JsonNode.class);
        return res.path("choices").get(0).path("message").path("content").asText().trim();
    }

    private JsonNode parse(String text) throws Exception {
        String cleaned = text.replace("```json", "").replace("```", "").trim();
        return mapper.readTree(cleaned);
    }

    public Map<String, Object> classifyUrgency(BloodRequest r) {
        try {
            String prompt = "Analyze this blood donation request and classify its urgency. Return ONLY valid JSON, no markdown.\n\n"
                + "Patient: " + r.getPatientName() + "\n"
                + "Blood type needed: " + r.getBloodType() + "\n"
                + "Units needed: " + r.getUnits() + "\n"
                + "Hospital: " + r.getHospital() + "\n"
                + "Notes: " + (r.getNotes() == null || r.getNotes().isBlank() ? "None" : r.getNotes()) + "\n\n"
                + "Return:\n{\n  \"urgency\": \"critical\" | \"urgent\" | \"normal\",\n"
                + "  \"reason\": \"one sentence explanation\",\n"
                + "  \"summary\": \"one engaging sentence for the live feed\"\n}";
            JsonNode j = parse(chat(prompt, "You are a helpful medical assistant AI. Always respond with valid JSON only."));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("urgency", j.path("urgency").asText("normal"));
            m.put("reason", j.path("reason").asText(""));
            m.put("summary", j.path("summary").asText(""));
            return m;
        } catch (Exception e) {
            System.err.println("Groq urgency error: " + e.getMessage());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("urgency", "normal");
            m.put("reason", "AI classification unavailable");
            m.put("summary", r.getBloodType() + " blood needed at " + r.getHospital());
            return m;
        }
    }

    public Map<String, Object> scoreDonorMatch(DonorProfile donor, BloodRequest request) {
        try {
            long days = donor.getLastDonated() != null
                ? Duration.between(donor.getLastDonated(), Instant.now()).toDays() : 999;
            String prompt = "Score this blood donor's compatibility for a request. Return ONLY valid JSON, no markdown.\n\n"
                + "Request: " + request.getBloodType() + " blood, " + request.getUrgency() + " urgency\n"
                + "Donor blood type: " + donor.getBloodType() + "\n"
                + "Donor health score: " + donor.getHealthScore() + "/100\n"
                + "Days since last donation: " + days + "\n"
                + "Donor total donations: " + donor.getTotalDonations() + "\n"
                + "Donor is verified: " + donor.isVerified() + "\n\n"
                + "Return:\n{\n  \"score\": 0-100,\n  \"compatible\": true | false,\n  \"reason\": \"brief explanation\"\n}";
            JsonNode j = parse(chat(prompt, "You are a helpful medical assistant AI. Always respond with valid JSON only."));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("score", j.path("score").asInt(50));
            m.put("compatible", j.path("compatible").asBoolean(true));
            m.put("reason", j.path("reason").asText(""));
            return m;
        } catch (Exception e) {
            System.err.println("Groq match error: " + e.getMessage());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("score", 50);
            m.put("compatible", true);
            m.put("reason", "AI scoring unavailable");
            return m;
        }
    }

    public Map<String, Object> generateHealthTip(DonorProfile p) {
        try {
            String prompt = "Generate a personalized post-donation health tip for a blood donor. Return ONLY valid JSON, no markdown.\n\n"
                + "Blood type: " + p.getBloodType() + "\n"
                + "Total donations: " + p.getTotalDonations() + "\n"
                + "Age: " + (p.getAge() > 0 ? p.getAge() : "unknown") + "\n\n"
                + "Return:\n{\n  \"tip\": \"one actionable health tip\",\n  \"title\": \"short title for the tip\"\n}";
            JsonNode j = parse(chat(prompt, "You are a medical health advisor. Respond with valid JSON only."));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tip", j.path("tip").asText(""));
            m.put("title", j.path("title").asText(""));
            return m;
        } catch (Exception e) {
            System.err.println("Groq health tip error: " + e.getMessage());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tip", "Stay hydrated and rest for 24 hours after donation.");
            m.put("title", "Post-donation care");
            return m;
        }
    }

    /** historical: list of maps with keys bloodType, city, count, critical, fulfilled */
    public Map<String, Object> forecastDemand(List<Map<String, Object>> historical) {
        try {
            StringBuilder sb = new StringBuilder();
            historical.stream().limit(15).forEach(d -> sb.append(d.get("bloodType")).append(" in ")
                .append(d.get("city")).append(": ").append(d.get("count")).append(" requests, ")
                .append(d.get("critical")).append(" critical, ").append(d.get("fulfilled"))
                .append(" fulfilled\n"));
            String prompt = "Based on 30 days of blood donation requests, forecast next week's demand. Return ONLY valid JSON, no markdown.\n\n"
                + "Historical data:\n" + sb + "\n"
                + "Return:\n{\n  \"predictions\": [\n    { \"bloodType\": \"O-\", \"city\": \"Bengaluru\", \"riskLevel\": \"high\"|\"medium\"|\"low\", \"reason\": \"brief reason\", \"recommendedDonors\": 5 }\n  ],\n"
                + "  \"summary\": \"one paragraph overall forecast\",\n  \"topRisk\": \"blood type most likely to be critical next week\"\n}";
            JsonNode j = parse(chat(prompt, "You are a medical supply forecasting AI. Respond with valid JSON only."));
            return mapper.convertValue(j, LinkedHashMap.class);
        } catch (Exception e) {
            System.err.println("Groq forecast error: " + e.getMessage());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("predictions", List.of());
            m.put("summary", "Forecast unavailable. Ensure Groq API key is configured.");
            m.put("topRisk", "O-");
            return m;
        }
    }
}
