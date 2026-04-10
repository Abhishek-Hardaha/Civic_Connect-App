package com.Abhiworks.civicconnect.models;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a user profile row.
 * reports_raised and total_upvotes are auto-managed by DB triggers/RPCs — never written by the app.
 */
public class UserProfile {

    @SerializedName("id")
    private String id;

    @SerializedName("username")
    private String username;

    @SerializedName("reports_raised")
    private int reportsRaised;

    @SerializedName("total_upvotes")
    private int totalUpvotes;

    @SerializedName("created_at")
    private String createdAt;

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getId()           { return id; }
    public String getUsername()     { return username; }
    public int getReportsRaised()   { return reportsRaised; }
    public int getTotalUpvotes()    { return totalUpvotes; }
    public String getCreatedAt()    { return createdAt; }
}
