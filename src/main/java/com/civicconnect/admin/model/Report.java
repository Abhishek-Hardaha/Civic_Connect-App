package com.civicconnect.admin.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Report {

    private String id;
    private String title;
    private String description;
    private String category;
    private String status;

    @JsonProperty("location_lat")
    private Double locationLat;

    @JsonProperty("location_lng")
    private Double locationLng;

    @JsonProperty("location_address")
    private String locationAddress;

    @JsonProperty("photo_url")
    private String photoUrl;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    // Helper for display
    public String getDisplayStatus() {
        if (status == null) return "REPORTED";
        return switch (status.toUpperCase()) {
            case "IN_PROGRESS" -> "IN PROGRESS";
            case "RESOLVED"    -> "RESOLVED";
            default            -> "REPORTED";
        };
    }

    public String getShortDate() {
        if (createdAt == null || createdAt.length() < 10) return "";
        return createdAt.substring(0, 10);
    }
}
