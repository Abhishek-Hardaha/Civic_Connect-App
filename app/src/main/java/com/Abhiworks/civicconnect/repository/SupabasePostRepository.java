package com.Abhiworks.civicconnect.repository;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.Abhiworks.civicconnect.models.CommunityPost;
import com.Abhiworks.civicconnect.service.SupabaseService;
import com.Abhiworks.civicconnect.utils.AppConstants;
import com.Abhiworks.civicconnect.utils.Callback;
import com.Abhiworks.civicconnect.utils.ImageUtils;
import com.Abhiworks.civicconnect.utils.JsonParser;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupabasePostRepository implements PostRepository {

    private final SupabaseService supabase;
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SupabasePostRepository(Context context) {
        this.context  = context.getApplicationContext();
        this.supabase = SupabaseService.getInstance(context);
    }

    // ── Feed ─────────────────────────────────────────────────────────────────

    @Override
    public void getPosts(Callback<List<CommunityPost>> callback) {
        String url = supabase.baseUrl + "/" + AppConstants.TABLE_POSTS
                + "?order=created_at.desc"
                + "&limit=30"
                + "&select=id,author_username,body,image_url,upvotes,created_at";
        supabase.get(url, new Callback<String>() {
            @Override public void onSuccess(String json) {
                callback.onSuccess(JsonParser.parsePostList(json));
            }
            @Override public void onError(Exception e) { callback.onError(e); }
        });
    }

    // ── Submit Post ──────────────────────────────────────────────────────────

    @Override
    public void submitPost(String userId, String authorUsername,
                           String body, String imageUrl,
                           Callback<Void> callback) {
        String url  = supabase.baseUrl + "/" + AppConstants.TABLE_POSTS;
        String json = CommunityPost.buildInsertJson(userId, authorUsername, body, imageUrl);
        supabase.post(url, json, new Callback<String>() {
            @Override public void onSuccess(String resp) { callback.onSuccess(null); }
            @Override public void onError(Exception e)   { callback.onError(e); }
        });
    }

    // ── Upvoting ─────────────────────────────────────────────────────────────

    @Override
    public void hasUpvoted(String userId, String postId, Callback<Boolean> callback) {
        String url = supabase.baseUrl + "/" + AppConstants.TABLE_UPVOTES
                + "?user_id=eq." + userId
                + "&post_id=eq." + postId
                + "&select=user_id";
        supabase.get(url, new Callback<String>() {
            @Override public void onSuccess(String json) {
                callback.onSuccess(JsonParser.hasRows(json));
            }
            @Override public void onError(Exception e) { callback.onError(e); }
        });
    }

    @Override
    public void upvotePost(String postId, String userId, Callback<Void> callback) {
        String url  = supabase.baseUrl + "/rpc/increment_upvotes";
        String body = "{\"p_post_id\":\"" + postId + "\",\"p_user_id\":\"" + userId + "\"}";
        supabase.post(url, body, new Callback<String>() {
            @Override public void onSuccess(String json) { callback.onSuccess(null); }
            @Override public void onError(Exception e)   { callback.onError(e); }
        });
    }

    // ── Image Upload ─────────────────────────────────────────────────────────

    @Override
    public void uploadPostImage(Uri imageUri, String userId, Callback<String> callback) {
        executor.execute(() -> {
            try {
                byte[] data = ImageUtils.compressBitmap(context, imageUri);
                String path = userId + "/" + UUID.randomUUID() + ".jpg";
                supabase.uploadFile(AppConstants.BUCKET_COMMUNITY, path, data, "image/jpeg", callback);
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e));
            }
        });
    }
}
