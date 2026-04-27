package com.Abhiworks.civicconnect.utils;

import com.Abhiworks.civicconnect.models.CommunityPost;
import com.Abhiworks.civicconnect.models.Issue;
import com.Abhiworks.civicconnect.models.UserProfile;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Static Gson-based parsers for all Supabase REST responses.
 * PostgREST always returns JSON arrays, even for single-row queries.
 */
public class JsonParser {

    private static final Gson GSON = new Gson();

    private JsonParser() {}

    public static List<Issue> parseIssueList(String json) {
        Type type = new TypeToken<List<Issue>>(){}.getType();
        List<Issue> list = GSON.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    public static List<CommunityPost> parsePostList(String json) {
        Type type = new TypeToken<List<CommunityPost>>(){}.getType();
        List<CommunityPost> list = GSON.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    public static List<com.Abhiworks.civicconnect.models.LeaderboardEntry> parseLeaderboardList(String json) {
        Type type = new TypeToken<List<com.Abhiworks.civicconnect.models.LeaderboardEntry>>(){}.getType();
        List<com.Abhiworks.civicconnect.models.LeaderboardEntry> list = GSON.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    /**
     * Parses the first element of a PostgREST array response as a UserProfile.
     * Returns null if the array is empty.
     */
    public static UserProfile parseProfile(String json) {
        Type type = new TypeToken<List<UserProfile>>(){}.getType();
        List<UserProfile> list = GSON.fromJson(json, type);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    /**
     * Extracts the "id" field from the first object in a PostgREST array response.
     * Used after INSERT with return=representation to get the generated UUID.
     */
    public static String parseId(String json) {
        try {
            JSONArray arr = new JSONArray(json);
            if (arr.length() > 0) {
                return arr.getJSONObject(0).optString("id", null);
            }
        } catch (Exception e) {
            // Try as plain object
            try {
                JSONObject obj = new JSONObject(json);
                return obj.optString("id", null);
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Parses the access_token from a Supabase Auth token response.
     */
    public static String parseAccessToken(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            return obj.optString("access_token", null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses the refresh_token from a Supabase Auth token response.
     */
    public static String parseRefreshToken(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            return obj.optString("refresh_token", null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses the user.id from a Supabase Auth token response.
     */
    public static String parseUserId(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            JSONObject user = obj.optJSONObject("user");
            if (user != null) return user.optString("id", null);
        } catch (Exception e) {
            // ignored
        }
        return null;
    }

    /** Returns true if a PostgREST array response has at least one element. */
    public static boolean hasRows(String json) {
        try {
            JSONArray arr = new JSONArray(json);
            return arr.length() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
