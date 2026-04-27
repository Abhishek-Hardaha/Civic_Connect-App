package com.Abhiworks.civicconnect.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Abhiworks.civicconnect.R;
import com.Abhiworks.civicconnect.adapters.LeaderboardAdapter;
import com.Abhiworks.civicconnect.models.LeaderboardEntry;
import com.Abhiworks.civicconnect.models.UserProfile;
import com.Abhiworks.civicconnect.repository.SupabaseIssueRepository;
import com.Abhiworks.civicconnect.service.SupabaseService;
import com.Abhiworks.civicconnect.session.UserSession;
import com.Abhiworks.civicconnect.utils.AppConstants;
import com.Abhiworks.civicconnect.utils.Callback;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvAvatar, tvUsername, tvCity, tvMemberSince;
    private TextView tvReportsRaised, tvUpvotes;
    private TextView tvMyRank, tvEmpty;
    private View cardMyRank, progress;
    private RecyclerView rvLeaderboard;

    private SupabaseIssueRepository issueRepo;
    private SupabaseService supabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        issueRepo = new SupabaseIssueRepository(this);
        supabase  = SupabaseService.getInstance(this);

        // Toolbar with back + logout menu
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Options menu for Logout
        toolbar.inflateMenu(R.menu.menu_profile);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                logout();
                return true;
            }
            return false;
        });

        // Bind views
        tvAvatar        = findViewById(R.id.tv_avatar);
        tvUsername      = findViewById(R.id.tv_username);
        tvCity          = findViewById(R.id.tv_city);
        tvMemberSince   = findViewById(R.id.tv_member_since);
        tvReportsRaised = findViewById(R.id.tv_reports_raised);
        tvUpvotes       = findViewById(R.id.tv_upvotes);
        cardMyRank      = findViewById(R.id.card_my_rank);
        tvMyRank        = findViewById(R.id.tv_my_rank);
        tvEmpty         = findViewById(R.id.tv_empty);
        rvLeaderboard   = findViewById(R.id.rv_leaderboard);
        progress        = findViewById(R.id.progress);

        rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        rvLeaderboard.setNestedScrollingEnabled(false);

        // Populate static session fields immediately (no network needed)
        populateHeaderFromSession();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
        loadLeaderboard();
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private void populateHeaderFromSession() {
        String username = UserSession.get().getUsername();
        String city     = UserSession.get().getCity();

        if (username != null && !username.isEmpty()) {
            tvAvatar.setText(String.valueOf(username.charAt(0)).toUpperCase());
            tvUsername.setText("@" + username);
        }
        if (city != null && !city.isEmpty()) {
            tvCity.setText("📍 " + city);
            tvCity.setVisibility(View.VISIBLE);
        } else {
            tvCity.setVisibility(View.GONE);
        }
    }

    // ── Profile (stats + joined date) ─────────────────────────────────────────

    private void loadProfile() {
        String uid = UserSession.get().getUserId();
        if (uid == null) return;

        showProgress(true);
        issueRepo.getProfile(uid, new Callback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile profile) {
                showProgress(false);
                if (profile == null) return;

                tvReportsRaised.setText(String.valueOf(profile.getReportsRaised()));
                tvUpvotes.setText(String.valueOf(profile.getTotalUpvotes()));

                // Update city if now available
                if (profile.getCity() != null) {
                    tvCity.setText("📍 " + profile.getCity());
                    tvCity.setVisibility(View.VISIBLE);
                    UserSession.get().setCity(profile.getCity());
                    getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
                            .edit().putString(AppConstants.PREF_CITY, profile.getCity()).apply();
                }

                // Member since date
                if (profile.getCreatedAt() != null) {
                    tvMemberSince.setText("Member since " + formatDate(profile.getCreatedAt()));
                }
            }
            @Override
            public void onError(Exception e) { showProgress(false); }
        });
    }

    // ── Leaderboard ───────────────────────────────────────────────────────────

    private void loadLeaderboard() {
        String city = UserSession.get().getCity();
        issueRepo.getLeaderboard(city, new Callback<List<LeaderboardEntry>>() {
            @Override
            public void onSuccess(List<LeaderboardEntry> entries) {
                if (entries.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvLeaderboard.setVisibility(View.GONE);
                    cardMyRank.setVisibility(View.GONE);
                    return;
                }
                tvEmpty.setVisibility(View.GONE);
                rvLeaderboard.setVisibility(View.VISIBLE);

                // Find current user's rank
                String me = UserSession.get().getUsername();
                for (LeaderboardEntry e : entries) {
                    if (e.getUsername() != null && e.getUsername().equals(me)) {
                        cardMyRank.setVisibility(View.VISIBLE);
                        tvMyRank.setText("#" + e.getRank() + " of " + entries.size() + " users"
                                + (city != null && !city.isEmpty() ? " in " + city : ""));
                        break;
                    }
                }

                LeaderboardAdapter adapter = new LeaderboardAdapter(entries, me);
                rvLeaderboard.setAdapter(adapter);
            }
            @Override
            public void onError(Exception e) {
                tvEmpty.setVisibility(View.VISIBLE);
                rvLeaderboard.setVisibility(View.GONE);
            }
        });
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    private void logout() {
        String url = supabase.authUrl + "/logout";
        supabase.post(url, "{}", new Callback<String>() {
            @Override public void onSuccess(String result) { clearAndGoLogin(); }
            @Override public void onError(Exception e)     { clearAndGoLogin(); }
        });
    }

    private void clearAndGoLogin() {
        SharedPreferences prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);
        UserSession.get().clear(prefs);
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finishAffinity();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showProgress(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /** Converts ISO 8601 timestamp to "Jan 15, 2025". */
    private String formatDate(String iso) {
        try {
            SimpleDateFormat in  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            SimpleDateFormat out = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            Date d = in.parse(iso);
            return d != null ? out.format(d) : iso;
        } catch (ParseException e) {
            // Fallback: return just the date part
            return iso.length() > 10 ? iso.substring(0, 10) : iso;
        }
    }
}
