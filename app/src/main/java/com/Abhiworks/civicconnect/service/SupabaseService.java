package com.Abhiworks.civicconnect.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.Abhiworks.civicconnect.BuildConfig;
import com.Abhiworks.civicconnect.session.UserSession;
import com.Abhiworks.civicconnect.utils.AppConstants;
import com.Abhiworks.civicconnect.utils.AuthException;
import com.Abhiworks.civicconnect.utils.Callback;
import com.Abhiworks.civicconnect.utils.NetworkException;
import com.Abhiworks.civicconnect.utils.StorageException;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Thread-safe singleton OkHttp client.
 *
 * Handles:
 *  - Attaching apikey + Authorization headers to every request
 *  - Silent 401 token refresh: on any 401, attempts POST /auth/v1/token?grant_type=refresh_token,
 *    updates UserSession + SharedPreferences, then retries the original request once.
 *    If the refresh itself fails → throws AuthException → activities route to LoginActivity.
 *  - JWT is never included in log output.
 */
public class SupabaseService {

    private static final String TAG = "SupabaseService";
    private static volatile SupabaseService instance;

    private final OkHttpClient http;
    public final String baseUrl;
    public final String storageUrl;
    public final String authUrl;
    public final String anonKey;

    private Context appContext;

    private SupabaseService(Context context) {
        this.appContext  = context.getApplicationContext();
        this.baseUrl     = BuildConfig.SUPABASE_URL + "/rest/v1";
        this.storageUrl  = BuildConfig.SUPABASE_URL + "/storage/v1";
        this.authUrl     = BuildConfig.SUPABASE_URL + "/auth/v1";
        this.anonKey     = BuildConfig.SUPABASE_ANON_KEY;

        // Logging interceptor — redacts Authorization header so JWT never appears in logs
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> {
            if (!message.startsWith("Authorization")) {
                Log.d(TAG, message);
            }
        });
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        logging.redactHeader("Authorization");

        this.http = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
    }

    public static SupabaseService getInstance(Context context) {
        if (instance == null) {
            synchronized (SupabaseService.class) {
                if (instance == null) {
                    instance = new SupabaseService(context);
                }
            }
        }
        return instance;
    }

    // ── Header Helpers ───────────────────────────────────────────────────────

    private Request.Builder authHeaders(Request.Builder builder) {
        String jwt = UserSession.get().getJwt();
        return builder
                .header("apikey", anonKey)
                .header("Authorization", "Bearer " + (jwt != null ? jwt : ""))
                .header("Content-Type", "application/json");
    }

    // ── Silent Token Refresh ─────────────────────────────────────────────────

    /**
     * Synchronously refreshes the access token using the stored refresh token.
     * Updates UserSession and SharedPreferences on success.
     * Throws AuthException if the refresh fails.
     */
    private void refreshTokenSync() throws IOException {
        String refreshToken = UserSession.get().getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new AuthException("No refresh token available");
        }

        String body = "{\"refresh_token\":\"" + refreshToken + "\"}";
        Request req = new Request.Builder()
                .url(authUrl + "/token?grant_type=refresh_token")
                .header("apikey", anonKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body, MediaType.parse("application/json")))
                .build();

        try (Response response = http.newCall(req).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new AuthException("Token refresh failed: HTTP " + response.code());
            }
            String json = response.body().string();
            JSONObject obj = new JSONObject(json);
            String newJwt     = obj.getString("access_token");
            String newRefresh = obj.getString("refresh_token");

            SharedPreferences prefs = appContext.getSharedPreferences(
                    AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
            UserSession.get().updateTokens(newJwt, newRefresh, prefs);
        } catch (Exception e) {
            if (e instanceof AuthException) throw (AuthException) e;
            throw new AuthException("Token refresh failed", e);
        }
    }

    // ── Core HTTP methods ────────────────────────────────────────────────────

    /**
     * Executes a request. On 401, silently refreshes the token and retries once.
     * All callbacks are posted to the main thread.
     */
    private void execute(Request request, boolean isRetry, Callback<String> cb) {
        http.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainThread(() -> cb.onError(new NetworkException("Network error: " + e.getMessage(), e)));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 401 && !isRetry) {
                    // Attempt silent token refresh then retry
                    try {
                        refreshTokenSync();
                    } catch (Exception refreshEx) {
                        mainThread(() -> cb.onError(new AuthException("Session expired. Please log in again.", refreshEx)));
                        return;
                    }
                    // Rebuild request with new JWT
                    Request retryReq = authHeaders(request.newBuilder()).build();
                    execute(retryReq, true, cb);
                    return;
                }

                if (response.code() == 401) {
                    mainThread(() -> cb.onError(new AuthException("Session expired. Please log in again.")));
                    return;
                }

                if (!response.isSuccessful()) {
                    String errBody = "";
                    try {
                        if (response.body() != null) errBody = response.body().string();
                    } catch (IOException ignored) {}
                    final String finalErr = errBody;
                    mainThread(() -> cb.onError(new NetworkException("HTTP " + response.code() + ": " + finalErr)));
                    return;
                }

                String body = "";
                try {
                    if (response.body() != null) body = response.body().string();
                } catch (IOException e) {
                    mainThread(() -> cb.onError(new NetworkException("Failed to read response body", e)));
                    return;
                }
                final String finalBody = body;
                mainThread(() -> cb.onSuccess(finalBody));
            }
        });
    }

    // ── Public API Methods ───────────────────────────────────────────────────

    /** POST to a REST/auth path with a JSON string body. */
    public void post(String url, String jsonBody, Callback<String> cb) {
        Request req = authHeaders(new Request.Builder().url(url))
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();
        execute(req, false, cb);
    }

    /** POST with Prefer: return=representation header (for insert + return). */
    public void postWithReturn(String url, String jsonBody, Callback<String> cb) {
        Request req = authHeaders(new Request.Builder().url(url))
                .header("Prefer", "return=representation")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();
        execute(req, false, cb);
    }

    /** GET a REST/auth path. */
    public void get(String url, Callback<String> cb) {
        Request req = authHeaders(new Request.Builder().url(url))
                .get()
                .build();
        execute(req, false, cb);
    }

    /** PATCH a REST path with a JSON string body. */
    public void patch(String url, String jsonBody, Callback<String> cb) {
        Request req = authHeaders(new Request.Builder().url(url))
                .patch(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();
        execute(req, false, cb);
    }

    /** Fetches the currently logged-in user's auth metadata (including email). */
    public void getUser(Callback<String> cb) {
        Request req = authHeaders(new Request.Builder().url(authUrl + "/user"))
                .get()
                .build();
        execute(req, false, cb);
    }

    /**
     * Upload a file to Supabase Storage via PUT.
     * Returns the public URL of the uploaded file via the callback.
     */
    public void uploadFile(String bucket, String path, byte[] data,
                           String mimeType, Callback<String> cb) {
        String uploadUrl = storageUrl + "/object/" + bucket + "/" + path;
        Request req = new Request.Builder()
                .url(uploadUrl)
                .header("apikey", anonKey)
                .header("Authorization", "Bearer " + UserSession.get().getJwt())
                .put(RequestBody.create(data, MediaType.parse(mimeType)))
                .build();

        http.newCall(req).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainThread(() -> cb.onError(new StorageException("Upload failed: " + e.getMessage(), e)));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String err = response.body() != null ? response.body().string() : "";
                    mainThread(() -> cb.onError(new StorageException("Upload failed HTTP " + response.code() + ": " + err)));
                    return;
                }
                // Build and return the public URL
                String publicUrl = BuildConfig.SUPABASE_URL
                        + "/storage/v1/object/public/" + bucket + "/" + path;
                mainThread(() -> cb.onSuccess(publicUrl));
            }
        });
    }

    /** Auth-specific POST — no Authorization header (used for login/signup). */
    public void postAuth(String url, String jsonBody, Callback<String> cb) {
        Request req = new Request.Builder()
                .url(url)
                .header("apikey", anonKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();
        http.newCall(req).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainThread(() -> cb.onError(new NetworkException("Network error: " + e.getMessage(), e)));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String err = response.body() != null ? response.body().string() : "";
                    mainThread(() -> cb.onError(new NetworkException("HTTP " + response.code() + ": " + err)));
                    return;
                }
                String body = response.body() != null ? response.body().string() : "";
                mainThread(() -> cb.onSuccess(body));
            }
        });
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    private void mainThread(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }
}
