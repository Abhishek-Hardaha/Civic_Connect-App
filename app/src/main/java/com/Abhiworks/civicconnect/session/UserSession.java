package com.Abhiworks.civicconnect.session;

import android.content.SharedPreferences;
import com.Abhiworks.civicconnect.utils.AppConstants;

/**
 * Singleton that holds the logged-in user's credentials in memory.
 * Initialized from SharedPreferences in SplashActivity on every app start.
 * All Activities read from here — they never access SharedPreferences directly.
 *
 * Session persistence rule:
 *   - Logged-in state is determined by the presence of a non-empty refresh_token.
 *   - The JWT may be expired; that is handled transparently by SupabaseService.
 */
public class UserSession {
    private static volatile UserSession instance;

    private String jwt;
    private String refreshToken;
    private String userId;
    private String email;
    private String username;
    private String city;

    private UserSession() {}

    public static UserSession get() {
        if (instance == null) {
            synchronized (UserSession.class) {
                if (instance == null) {
                    instance = new UserSession();
                }
            }
        }
        return instance;
    }

    // ── Init / Persistence ──────────────────────────────────────────────────

    /** Called once per app start in SplashActivity to restore session from disk. */
    public void loadFromPrefs(SharedPreferences prefs) {
        jwt          = prefs.getString(AppConstants.PREF_JWT, null);
        refreshToken = prefs.getString(AppConstants.PREF_REFRESH, null);
        userId       = prefs.getString(AppConstants.PREF_USER_ID, null);
        email        = prefs.getString(AppConstants.PREF_EMAIL, null);
        username     = prefs.getString(AppConstants.PREF_USERNAME, null);
        city         = prefs.getString(AppConstants.PREF_CITY, null);
    }

    /** Called after login or signup to persist the session. */
    public void init(String jwt, String refresh, String userId, String email, String username, String city) {
        this.jwt          = jwt;
        this.refreshToken = refresh;
        this.userId       = userId;
        this.email        = email;
        this.username     = username;
        this.city         = city;
    }

    /** Called after init() to persist values to disk. */
    public void saveToPrefs(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(AppConstants.PREF_JWT,      jwt);
        editor.putString(AppConstants.PREF_REFRESH,  refreshToken);
        editor.putString(AppConstants.PREF_USER_ID,  userId);
        editor.putString(AppConstants.PREF_EMAIL,    email);
        editor.putString(AppConstants.PREF_USERNAME, username);
        editor.putString(AppConstants.PREF_CITY,     city);
        editor.apply();
    }

    /** Called after a silent token refresh to update JWT and refresh token. */
    public void updateTokens(String newJwt, String newRefresh, SharedPreferences prefs) {
        this.jwt          = newJwt;
        this.refreshToken = newRefresh;
        prefs.edit()
             .putString(AppConstants.PREF_JWT,     newJwt)
             .putString(AppConstants.PREF_REFRESH, newRefresh)
             .apply();
    }

    /** Called on explicit logout — wipes everything. */
    public void clear(SharedPreferences prefs) {
        jwt          = null;
        refreshToken = null;
        userId       = null;
        email        = null;
        username     = null;
        city         = null;
        prefs.edit().clear().apply();
    }

    // ── Session State ────────────────────────────────────────────────────────

    /**
     * True if a refresh token is present. The JWT may be expired — that is fine.
     * SupabaseService will transparently refresh it on the next API call.
     */
    public boolean isLoggedIn() {
        return refreshToken != null && !refreshToken.isEmpty();
    }

    public boolean hasUsername() {
        return username != null && !username.trim().isEmpty();
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getJwt()          { return jwt; }
    public String getRefreshToken() { return refreshToken; }
    public String getUserId()       { return userId; }
    public String getEmail()        { return email; }
    public String getUsername()     { return username; }
    public String getCity()         { return city; }

    // ── Setters (limited) ────────────────────────────────────────────────────

    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email)       { this.email = email; }
    public void setCity(String city)         { this.city = city; }
    public void setJwt(String jwt)           { this.jwt = jwt; }
}
