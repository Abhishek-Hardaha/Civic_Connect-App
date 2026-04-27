package com.Abhiworks.civicconnect.utils;

public final class AppConstants {

    // --- Supabase Tables ---
    public static final String TABLE_ISSUES    = "issues";
    public static final String TABLE_PROFILES  = "profiles";
    public static final String TABLE_POSTS     = "community_posts";
    public static final String TABLE_UPVOTES   = "post_upvote_log";

    // --- Supabase Storage Buckets ---
    public static final String BUCKET_ISSUES      = "issue-images";
    public static final String BUCKET_COMMUNITY   = "community-images";
    public static final String BUCKET_RESOLUTION  = "resolution-photos";

    // --- SharedPreferences ---
    public static final String PREFS_NAME       = "civic_connect_auth";
    public static final String PREF_JWT         = "jwt";
    public static final String PREF_REFRESH     = "refresh_token";
    public static final String PREF_USER_ID     = "user_id";
    public static final String PREF_USERNAME    = "username";
    public static final String PREF_CITY        = "city";
    public static final String PREF_THEME_MODE  = "theme_mode";

    // --- Theme Mode Values ---
    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT  = "light";
    public static final String THEME_DARK   = "dark";

    // --- Image Processing ---
    public static final int MAX_IMAGE_SIZE_PX = 1280;
    public static final int JPEG_QUALITY      = 80;

    // --- Issue Categories (exact strings matching DB CHECK constraint) ---
    public static final String[] CATEGORIES = {
            "Road/Transport",
            "Garbage/Pollution",
            "Water & Drainage",
            "Electricity & Lighting",
            "Public Nuisance & Safety",
            "Animal",
            "Other"
    };

    // --- Issue Statuses ---
    public static final String STATUS_PENDING     = "pending";
    public static final String STATUS_IN_PROGRESS = "in_progress";
    public static final String STATUS_RESOLVED    = "resolved";
    public static final String STATUS_REJECTED    = "rejected";

    // --- Intent Extras ---
    public static final String EXTRA_ISSUE_ID = "extra_issue_id";

    private AppConstants() {}
}
