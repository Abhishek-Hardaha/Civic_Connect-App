package com.Abhiworks.civicconnect.models;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a single row in the city leaderboard.
 * Populated from the profiles table ordered by reports_raised DESC.
 * rank is computed client-side (1-indexed position in the list).
 */
public class LeaderboardEntry {

    /** 1-indexed rank, assigned by the adapter after fetching. */
    private int rank;

    @SerializedName("username")
    private String username;

    @SerializedName("reports_raised")
    private int reportsRaised;

    @SerializedName("total_upvotes")
    private int totalUpvotes;

    @SerializedName("city")
    private String city;

    // Getters
    public int getRank()            { return rank; }
    public String getUsername()     { return username; }
    public int getReportsRaised()   { return reportsRaised; }
    public int getTotalUpvotes()    { return totalUpvotes; }
    public String getCity()         { return city; }

    // Setters
    public void setRank(int rank)           { this.rank = rank; }
    public void setReportsRaised(int count) { this.reportsRaised = count; }
    public void setTotalUpvotes(int count)  { this.totalUpvotes = count; }
}
