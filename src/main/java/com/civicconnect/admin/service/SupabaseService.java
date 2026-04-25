package com.civicconnect.admin.service;

import com.civicconnect.admin.model.Report;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SupabaseService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;   // bypasses RLS — sees ALL reports

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Headers ──────────────────────────────────────────────────────────
    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.set("apikey",        serviceRoleKey);
        h.set("Authorization", "Bearer " + serviceRoleKey);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    // ── GET all reports, newest first ────────────────────────────────────
    public List<Report> getAllReports() {
        String url = supabaseUrl + "/rest/v1/reports?order=created_at.desc";
        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers()), String.class);
            return objectMapper.readValue(resp.getBody(), new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to fetch reports", e);
            return List.of();
        }
    }

    // ── GET reports filtered by status ───────────────────────────────────
    public List<Report> getReportsByStatus(String status) {
        String url = supabaseUrl + "/rest/v1/reports?status=eq." + status + "&order=created_at.desc";
        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers()), String.class);
            return objectMapper.readValue(resp.getBody(), new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to fetch reports by status", e);
            return List.of();
        }
    }

    // ── GET reports filtered by category ─────────────────────────────────
    public List<Report> getReportsByCategory(String category) {
        String url = supabaseUrl + "/rest/v1/reports?category=eq." + category + "&order=created_at.desc";
        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers()), String.class);
            return objectMapper.readValue(resp.getBody(), new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to fetch reports by category", e);
            return List.of();
        }
    }

    // ── PATCH — update report status ─────────────────────────────────────
    public boolean updateStatus(String reportId, String newStatus) {
        String url = supabaseUrl + "/rest/v1/reports?id=eq." + reportId;
        try {
            HttpHeaders h = headers();
            h.set("Prefer", "return=minimal");
            Map<String, String> body = Map.of("status", newStatus);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, h);
            ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.PATCH, entity, String.class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Failed to update status for report {}", reportId, e);
            return false;
        }
    }

    // ── Stats ─────────────────────────────────────────────────────────────
    public long countByStatus(String status) {
        String url = supabaseUrl + "/rest/v1/reports?status=eq." + status + "&select=id";
        try {
            HttpHeaders h = headers();
            h.set("Prefer", "count=exact");
            ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(h), String.class);
            // Supabase returns count in Content-Range header: "0-N/TOTAL"
            String contentRange = resp.getHeaders().getFirst("Content-Range");
            if (contentRange != null && contentRange.contains("/")) {
                return Long.parseLong(contentRange.split("/")[1]);
            }
            // Fallback: count from body
            List<?> list = objectMapper.readValue(resp.getBody(), List.class);
            return list.size();
        } catch (Exception e) {
            log.error("Failed to count reports by status {}", status, e);
            return 0;
        }
    }

    public long countAll() {
        String url = supabaseUrl + "/rest/v1/reports?select=id";
        try {
            HttpHeaders h = headers();
            h.set("Prefer", "count=exact");
            ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(h), String.class);
            String contentRange = resp.getHeaders().getFirst("Content-Range");
            if (contentRange != null && contentRange.contains("/")) {
                return Long.parseLong(contentRange.split("/")[1]);
            }
            List<?> list = objectMapper.readValue(resp.getBody(), List.class);
            return list.size();
        } catch (Exception e) {
            log.error("Failed to count all reports", e);
            return 0;
        }
    }
}
