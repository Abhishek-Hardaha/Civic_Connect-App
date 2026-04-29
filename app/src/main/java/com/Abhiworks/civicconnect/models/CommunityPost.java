package com.Abhiworks.civicconnect.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a community feed post.
 * author_username is denormalized — copied from profiles at insert time to avoid a JOIN.
 */
public class CommunityPost implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("author_username")
    private String authorUsername;

    @SerializedName("body")
    private String body;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("upvotes")
    private int upvotes;

    @SerializedName("created_at")
    private String createdAt;

    // ── Transient UI state (not from DB) ─────────────────────────────────────
    private transient boolean votedByCurrentUser = false;

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getId()             { return id; }
    public String getUserId()         { return userId; }
    public String getAuthorUsername() { return authorUsername; }
    public String getBody()           { return body; }
    public String getImageUrl()       { return imageUrl; }
    public int getUpvotes()           { return upvotes; }
    public String getCreatedAt()      { return createdAt; }
    public boolean isVotedByCurrentUser() { return votedByCurrentUser; }

    // ── Setters (UI state only) ───────────────────────────────────────────────

    public void setVotedByCurrentUser(boolean voted) { this.votedByCurrentUser = voted; }
    public void incrementUpvotes()                   { this.upvotes++; }
    public void decrementUpvotes()                   { this.upvotes = Math.max(0, this.upvotes - 1); }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public boolean hasImage() { return imageUrl != null && !imageUrl.isEmpty(); }

    /** Builds the JSON body for a new post INSERT. */
    public static String buildInsertJson(String userId, String authorUsername,
                                         String body, String imageUrl) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"user_id\":\"").append(userId).append("\"");
        sb.append(",\"author_username\":\"").append(escape(authorUsername)).append("\"");
        sb.append(",\"body\":\"").append(escape(body)).append("\"");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            sb.append(",\"image_url\":\"").append(escape(imageUrl)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommunityPost)) return false;
        return Objects.equals(id, ((CommunityPost) o).id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
