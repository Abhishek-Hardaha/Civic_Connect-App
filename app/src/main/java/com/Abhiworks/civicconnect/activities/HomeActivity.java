package com.Abhiworks.civicconnect.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Abhiworks.civicconnect.R;
import com.Abhiworks.civicconnect.adapters.RecentIssueAdapter;
import com.Abhiworks.civicconnect.models.Issue;
import com.Abhiworks.civicconnect.models.UserProfile;
import com.Abhiworks.civicconnect.repository.SupabaseIssueRepository;
import com.Abhiworks.civicconnect.service.SupabaseService;
import com.Abhiworks.civicconnect.session.UserSession;
import com.Abhiworks.civicconnect.utils.AppConstants;
import com.Abhiworks.civicconnect.utils.Callback;

import java.util.Calendar;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private TextView tvGreeting, tvReportsRaised, tvUpvotes, tvPendingBadge, tvAvatar;
    private SupabaseIssueRepository issueRepo;
    private SupabaseService supabase;
    private RecyclerView rvRecent;
    private RecentIssueAdapter recentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        issueRepo = new SupabaseIssueRepository(this);
        supabase  = SupabaseService.getInstance(this);

        tvGreeting      = findViewById(R.id.tv_greeting);
        tvReportsRaised = findViewById(R.id.tv_reports_raised);
        tvUpvotes       = findViewById(R.id.tv_upvotes);
        tvPendingBadge  = findViewById(R.id.tv_reports_count);
        tvAvatar        = findViewById(R.id.btn_avatar);

        // Set avatar initial from username
        String username = UserSession.get().getUsername();
        if (username != null && !username.isEmpty()) {
            tvAvatar.setText(String.valueOf(username.charAt(0)).toUpperCase());
        }

        // Card click listeners
        findViewById(R.id.card_report).setOnClickListener(v ->
                startActivity(new Intent(this, ReportIssueActivity.class)));
        findViewById(R.id.card_community).setOnClickListener(v ->
                startActivity(new Intent(this, CommunityActivity.class)));
        findViewById(R.id.card_my_reports).setOnClickListener(v ->
                startActivity(new Intent(this, MyReportsActivity.class)));
        findViewById(R.id.btn_view_all).setOnClickListener(v ->
                startActivity(new Intent(this, MyReportsActivity.class)));

        // Avatar → ProfileActivity
        tvAvatar.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        // Theme toggle
        findViewById(R.id.btn_theme).setOnClickListener(v -> showThemeDialog());

        // Recent Reports List
        rvRecent = findViewById(R.id.rv_recent_reports);
        rvRecent.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recentAdapter = new RecentIssueAdapter(issue -> {
            Intent intent = new Intent(this, IssueDetailActivity.class);
            intent.putExtra(AppConstants.EXTRA_ISSUE_ID, issue.getId());
            startActivity(intent);
        });
        rvRecent.setAdapter(recentAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGreeting();
        fetchStats();
        fetchRecentReports();
    }

    // ── Greeting ─────────────────────────────────────────────────────────────

    private void updateGreeting() {
        String username = UserSession.get().getUsername();
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String timeOfDay = hour < 12 ? "morning" : hour < 17 ? "afternoon" : "evening";
        tvGreeting.setText("Good " + timeOfDay + ", " + (username != null ? username : "there") + " 👋");
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    private void fetchStats() {
        String uid = UserSession.get().getUserId();
        if (uid == null) return;

        issueRepo.getProfile(uid, new Callback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile profile) {
                if (profile == null) return;
                tvReportsRaised.setText(String.valueOf(profile.getReportsRaised()));
                tvUpvotes.setText(String.valueOf(profile.getTotalUpvotes()));
                
                // Persist city in session if freshly fetched
                if (profile.getCity() != null && UserSession.get().getCity() == null) {
                    UserSession.get().setCity(profile.getCity());
                    getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
                            .edit().putString(AppConstants.PREF_CITY, profile.getCity()).apply();
                }
            }
            @Override public void onError(Exception e) { /* decorative — silent */ }
        });

        // Pending badge
        issueRepo.getMyIssues(uid, new Callback<List<Issue>>() {
            @Override
            public void onSuccess(List<Issue> issues) {
                long pending = issues.stream()
                        .filter(i -> AppConstants.STATUS_PENDING.equals(i.getStatus()))
                        .count();
                if (pending > 0) {
                    tvPendingBadge.setVisibility(View.VISIBLE);
                    tvPendingBadge.setText(pending + " pending");
                } else {
                    tvPendingBadge.setVisibility(View.GONE);
                }
            }
            @Override public void onError(Exception e) {}
        });
    }

    // ── Recent Reports ────────────────────────────────────────────────────────

    private void fetchRecentReports() {
        String uid = UserSession.get().getUserId();
        if (uid == null) return;

        issueRepo.getRecentIssues(uid, 5, new Callback<List<Issue>>() {
            @Override
            public void onSuccess(List<Issue> issues) {
                recentAdapter.submitList(issues);
                rvRecent.setVisibility(issues.isEmpty() ? View.GONE : View.VISIBLE);
            }
            @Override public void onError(Exception e) {
                rvRecent.setVisibility(View.GONE);
            }
        });
    }

    // ── Theme Dialog ──────────────────────────────────────────────────────────

    private void showThemeDialog() {
        SharedPreferences prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);
        String current = prefs.getString(AppConstants.PREF_THEME_MODE, AppConstants.THEME_SYSTEM);

        String[] options = {"System Default", "Light", "Dark"};
        String[] values  = {AppConstants.THEME_SYSTEM, AppConstants.THEME_LIGHT, AppConstants.THEME_DARK};

        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) { checked = i; break; }
        }

        new AlertDialog.Builder(this)
                .setTitle("Theme")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    String chosen = values[which];
                    prefs.edit().putString(AppConstants.PREF_THEME_MODE, chosen).apply();
                    int mode;
                    switch (chosen) {
                        case AppConstants.THEME_LIGHT: mode = AppCompatDelegate.MODE_NIGHT_NO; break;
                        case AppConstants.THEME_DARK:  mode = AppCompatDelegate.MODE_NIGHT_YES; break;
                        default: mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                    }
                    AppCompatDelegate.setDefaultNightMode(mode);
                    dialog.dismiss();
                    recreate();
                })
                .show();
    }
}
