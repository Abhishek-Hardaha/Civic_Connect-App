package com.Abhiworks.civicconnect.models;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Represents a civic issue report.
 * Field names use @SerializedName to match exact Supabase snake_case column names.
 * Status is set only by the admin dashboard — the app only reads and displays it.
 */
public class Issue implements java.io.Serializable {

    private static final List<String> VALID_STATUSES = Arrays.asList(
            "pending", "in_progress", "resolved", "rejected"
    );

    // ── Fields ───────────────────────────────────────────────────────────────

    @SerializedName("id")
    private String id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("category")
    private String category;

    @SerializedName("status")
    private String status;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    @SerializedName("locality")
    private String locality;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("resolution_note")
    private String resolutionNote;

    @SerializedName("resolution_photo_url")
    private String resolutionPhotoUrl;

    @SerializedName("resolved_at")
    private String resolvedAt;

    // ── Constructor (use Builder instead) ────────────────────────────────────

    private Issue() {}

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getId()               { return id; }
    public String getUserId()           { return userId; }
    public String getTitle()            { return title; }
    public String getDescription()      { return description; }
    public String getCategory()         { return category; }
    public String getStatus()           { return status; }
    public String getImageUrl()         { return imageUrl; }
    public Double getLatitude()         { return latitude; }
    public Double getLongitude()        { return longitude; }
    public String getLocality()         { return locality; }
    public String getCreatedAt()        { return createdAt; }
    public String getUpdatedAt()        { return updatedAt; }
    public String getResolutionNote()   { return resolutionNote; }
    public String getResolutionPhotoUrl() { return resolutionPhotoUrl; }
    public String getResolvedAt()       { return resolvedAt; }

    // ── Helper Booleans ──────────────────────────────────────────────────────

    public boolean isResolved()        { return "resolved".equals(status); }
    public boolean hasResolutionNote() { return resolutionNote != null && !resolutionNote.isEmpty(); }
    public boolean hasResolutionPhoto(){ return resolutionPhotoUrl != null && !resolutionPhotoUrl.isEmpty(); }
    public boolean hasImage()          { return imageUrl != null && !imageUrl.isEmpty(); }
    public boolean hasLocation()       { return latitude != null && longitude != null; }

    // ── Status Mutation ──────────────────────────────────────────────────────

    /**
     * Updates status, validating against allowed values.
     * Used only to update the local copy in the adapter.
     */
    public void updateStatus(String newStatus) {
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid status: " + newStatus
                    + ". Allowed: " + VALID_STATUSES);
        }
        this.status = newStatus;
    }

    // ── Object Overrides ─────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Issue)) return false;
        return Objects.equals(id, ((Issue) o).id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }

    @Override
    public String toString() {
        return "Issue{id='" + id + "', title='" + title + "', status='" + status + "'}";
    }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static class Builder {
        private final String title;
        private final String category;
        private final String userId;
        private String description;
        private String imageUrl;
        private Double latitude;
        private Double longitude;
        private String locality;

        /** Required fields. */
        public Builder(String title, String category, String userId) {
            this.title    = title;
            this.category = category;
            this.userId   = userId;
        }

        public Builder description(String d) { this.description = d; return this; }
        public Builder imageUrl(String u)    { this.imageUrl = u; return this; }
        public Builder latitude(Double lat)  { this.latitude = lat; return this; }
        public Builder longitude(Double lon) { this.longitude = lon; return this; }
        public Builder locality(String loc)  { this.locality = loc; return this; }

        /**
         * Builds the Gson-serialisable JSON string for the INSERT body.
         * The server sets id, created_at, updated_at, and status automatically.
         */
        public String toJson() {
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"user_id\":\"").append(userId).append("\"");
            sb.append(",\"title\":\"").append(escape(title)).append("\"");
            sb.append(",\"category\":\"").append(escape(category)).append("\"");
            if (description != null)
                sb.append(",\"description\":\"").append(escape(description)).append("\"");
            if (imageUrl != null)
                sb.append(",\"image_url\":\"").append(escape(imageUrl)).append("\"");
            if (latitude != null)
                sb.append(",\"latitude\":").append(latitude);
            if (longitude != null)
                sb.append(",\"longitude\":").append(longitude);
            if (locality != null)
                sb.append(",\"locality\":\"").append(escape(locality)).append("\"");
            sb.append("}");
            return sb.toString();
        }

        private String escape(String s) {
            return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}
