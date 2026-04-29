package com.Abhiworks.civicconnect.repository;

import android.content.Context;
import android.net.Uri;

import com.Abhiworks.civicconnect.models.CommunityPost;
import com.Abhiworks.civicconnect.models.Issue;
import com.Abhiworks.civicconnect.models.LeaderboardEntry;
import com.Abhiworks.civicconnect.models.UserProfile;
import com.Abhiworks.civicconnect.service.SupabaseService;
import com.Abhiworks.civicconnect.utils.AppConstants;
import com.Abhiworks.civicconnect.utils.Callback;
import com.Abhiworks.civicconnect.utils.ImageUtils;
import com.Abhiworks.civicconnect.utils.JsonParser;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Supabase implementation of IssueRepository.
 * All network calls use SupabaseService (OkHttp .enqueue()).
 * Image compression uses a single-thread ExecutorService.
 */
public class SupabaseIssueRepository implements IssueRepository {

    private final SupabaseService supabase;
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SupabaseIssueRepository(Context context) {
        this.context  = context.getApplicationContext();
        this.supabase = SupabaseService.getInstance(context);
    }

    // ── Issues ───────────────────────────────────────────────────────────────

    @Override
    public void getMyIssues(String userId, Callback<List<Issue>> callback) {
        String url = supabase.baseUrl + "/" + AppConstants.TABLE_ISSUES
                + "?user_id=eq." + userId
                + "&order=created_at.desc"
                + "&select=id,title,status,category,image_url,resolution_note,"
                + "resolution_photo_url,resolved_at,created_at";
        supabase.get(url, new Callback<String>() {
            @Override public void onSuccess(String json) {
                callback.onSuccess(JsonParser.parseIssueList(json));
            }
            @Override public void onError(Exception e) { callback.onError(e); }
        });
    }

    @Override
    public void getIssueById(String issueId, Callback<Issue> callback) {
        String url = supabase.baseUrl + "/" + AppConstants.TABLE_ISSUES
                + "?id=eq." + issueId + "&select=*";
        supabase.get(url, new Callback<String>() {
            @Override public void onSuccess(String json) {
                List<Issue> list = JsonParser.parseIssueList(json);
                if (list.isEmpty()) {
                    callback.onError(new Exception("Issue not found"));
                } else {
                    callback.onSuccess(list.get(0));
                }
            }
            @Override public void onError(Exception e) { callback.onError(e); }
        });
    }

    @Override
    public void submitIssue(Issue.Builder builder, Callback<Issue> callback) {
        String url  = supabase.baseUrl + "/" + AppConstants.TABLE_ISSUES;
        String body = builder.toJson();
        supabase.postWithReturn(url, body, new Callback<String>() {
            @Override public void onSuccess(String json) {
                List<Issue> list = JsonParser.parseIssueList(json);
                if (list.isEmpty()) {
                    callback.onError(new Exception("No issue returned from server"));
                } else {
                    callback.onSuccess(list.get(0));
                }
            }
            @Override public void onError(Exception e) { callback.onError(e); }
        });
    }

    /**
     * Uploads an image in the background then calls the given callback with the public URL.
     * Image compression also happens on the background thread.
     */
    public void uploadIssueImage(Uri imageUri, String userId, Callback<String> callback) {
        executor.execute(() -> {
            try {
                byte[] data = ImageUtils.compressBitmap(context, imageUri);
                String path = userId + "/" + UUID.randomUUID() + ".jpg";
                supabase.uploadFile(AppConstants.BUCKET_ISSUES, path, data, "image/jpeg", callback);
            } catch (Exception e) {
                // Already on background thread; Callback.onError posts to main inside SupabaseService
                // But here we call directly, so post to main manually isn't needed since
                // SupabaseService.uploadFile handles it — but this path bypasses it, so:
                new android.os.Handler(android.os.Looper.getMainLooper())
                        .post(() -> callback.onError(e));
            }
        });
    }

    // ── Profiles ─────────────────────────────────────────────────────────────

    @Override
    public void getProfile(String userId, Callback<UserProfile> callback) {
        String url = supabase.baseUrl + "/" + AppConstants.TABLE_PROFILES
                + "?id=eq." + userId
                + "&select=username,city,reports_raised,total_upvotes,issues_resolved,created_at";
        supabase.get(url, new Callback<String>() {
            @Override public void onSuccess(String json) {
                UserProfile profile = JsonParser.parseProfile(json);
                callback.onSuccess(profile);
            }
            @Override public void onError(Exception e) { callback.onError(e); }
        });
    }

    // fetchRealStats removed because database is source of truth

    @Override
    public void checkUsernameAvailable(String username, Callback<Boolean> callback) {
        String url = supabase.baseUrl + "/" + AppConstants.TABLE_PROFILES
                + "?username=eq." + username + "&select=id";
        supabase.get(url, new Callback<String>() {
            @Override public void onSuccess(String json) {
                // Available if the array is empty (no matching username found)
                callback.onSuccess(!JsonParser.hasRows(json));
            }
            @Override public void onError(Exception e) { callback.onError(e); }
        });
    }

    @Override
    public void setUsername(String userId, String username, Callback<Void> callback) {
        String url  = supabase.baseUrl + "/" + AppConstants.TABLE_PROFILES
                + "?id=eq." + userId;
        String body = "{\"username\":\"" + username + "\"}";
        supabase.patch(url, body, new Callback<String>() {
            @Override public void onSuccess(String json) { callback.onSuccess(null); }
            @Override public void onError(Exception e) { callback.onError(e); }
        });
    }

    // ── Leaderboard ──────────────────────────────────────────────────────────

    /**
     * Fetches top 50 profiles ordered by reports_raised descending.
     * If city is non-null and non-empty, filters to that city.
     * Rank is assigned client-side after parsing.
     */
    @Override
    public void getLeaderboard(String city, Callback<java.util.List<LeaderboardEntry>> callback) {
        String url = supabase.baseUrl + "/rpc/get_city_leaderboard";
        String body = "{}";
        if (city != null && !city.trim().isEmpty()) {
            body = "{\"p_city\":\"" + city.trim() + "\"}";
        }
        supabase.postWithReturn(url, body, new Callback<String>() {
            @Override public void onSuccess(String json) {
                java.util.List<LeaderboardEntry> list = JsonParser.parseLeaderboardList(json);
                for (int i = 0; i < list.size(); i++) list.get(i).setRank(i + 1);
                callback.onSuccess(list);
            }
            @Override public void onError(Exception e) {
                // If it fails with p_city, try with city parameter or no parameter
                callback.onError(e);
            }
        });
    }

    // ── Recent Reports ───────────────────────────────────────────────────────
 
    /**
     * Fetches up to 'limit' most recent issues for the user.
     */
    @Override
    public void getRecentIssues(String userId, int limit, Callback<List<Issue>> callback) {
        String url = supabase.baseUrl + "/" + AppConstants.TABLE_ISSUES
                + "?user_id=eq." + userId
                + "&select=id,title,status,category,image_url,created_at"
                + "&order=created_at.desc"
                + "&limit=" + limit;
        supabase.get(url, new Callback<String>() {
            @Override public void onSuccess(String json) {
                callback.onSuccess(JsonParser.parseIssueList(json));
            }
            @Override public void onError(Exception e) { callback.onError(e); }
        });
    }
}
