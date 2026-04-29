package com.Abhiworks.civicconnect.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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

    private TextView tvAvatar, tvUsername, tvEmail, tvCity, tvMemberSince;
    private TextView tvReportsRaised, tvResolved, tvUpvotes;
    private TextView tvMyRank, tvEmpty;
    private View cardMyRank, progress;
    private RecyclerView rvLeaderboard;

    private SupabaseIssueRepository issueRepo;
    private SupabaseService supabase;
    private UserProfile currentProfile;

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
        tvEmail         = findViewById(R.id.tv_email);
        tvCity          = findViewById(R.id.tv_city);
        tvMemberSince   = findViewById(R.id.tv_member_since);
        tvReportsRaised = findViewById(R.id.tv_reports_raised);
        tvResolved      = findViewById(R.id.tv_resolved);
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

        findViewById(R.id.btn_edit_username).setOnClickListener(v -> showEditUsernameDialog());
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
        String email    = UserSession.get().getEmail();

        if (username != null && !username.isEmpty()) {
            tvAvatar.setText(String.valueOf(username.charAt(0)).toUpperCase());
            tvUsername.setText("@" + username);
        }

        // Show email if available, else try to fetch it
        if (email != null && !email.isEmpty()) {
            tvEmail.setText(email);
            tvEmail.setVisibility(View.VISIBLE);
        } else {
            tvEmail.setVisibility(View.GONE);
            fetchAuthEmail();
        }

        if (city != null && !city.isEmpty()) {
            tvCity.setText("📍 " + city);
            tvCity.setVisibility(View.VISIBLE);
        } else {
            tvCity.setVisibility(View.GONE);
        }
    }

    private void fetchAuthEmail() {
        supabase.getUser(new Callback<String>() {
            @Override
            public void onSuccess(String json) {
                String email = com.Abhiworks.civicconnect.utils.JsonParser.parseEmail(json);
                if (email != null && !email.isEmpty()) {
                    UserSession.get().setEmail(email);
                    tvEmail.setText(email);
                    tvEmail.setVisibility(View.VISIBLE);
                    
                    // Persist it so we don't have to fetch it next time
                    getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
                            .edit().putString(AppConstants.PREF_EMAIL, email).apply();
                }
            }
            @Override public void onError(Exception e) { 
                Log.e("ProfileActivity", "Failed to fetch auth email", e);
            }
        });
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
                if (profile == null) {
                    Toast.makeText(ProfileActivity.this, "Profile not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                currentProfile = profile;

                tvReportsRaised.setText(String.valueOf(profile.getReportsRaised()));
                tvResolved.setText(String.valueOf(profile.getIssuesResolved()));
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
            public void onError(Exception e) {
                showProgress(false);
                Toast.makeText(ProfileActivity.this, "Error loading stats: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
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

                // Find current user's rank and update stats from the fresh profile
                String me = UserSession.get().getUsername();
                for (LeaderboardEntry e : entries) {
                    if (e.getUsername() != null && e.getUsername().equals(me)) {
                        // Inject fresh stats into the leaderboard entry for the current user
                        if (currentProfile != null) {
                            e.setReportsRaised(currentProfile.getReportsRaised());
                            e.setTotalUpvotes(currentProfile.getTotalUpvotes());
                        }

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

    // ── Edit Username ────────────────────────────────────────────────────────

    private void showEditUsernameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Username");

        final EditText input = new EditText(this);
        input.setHint("Enter new username");
        input.setText(UserSession.get().getUsername());
        
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(60, 20, 60, 0);
        input.setLayoutParams(params);
        container.addView(input);
        
        builder.setView(container);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newUsername = input.getText().toString().trim();
            if (newUsername.isEmpty()) {
                Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newUsername.equals(UserSession.get().getUsername())) return;

            checkAndSetUsername(newUsername);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void checkAndSetUsername(String newUsername) {
        showProgress(true);
        issueRepo.checkUsernameAvailable(newUsername, new Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean available) {
                if (available) {
                    performUsernameUpdate(newUsername);
                } else {
                    showProgress(false);
                    Toast.makeText(ProfileActivity.this, "Username already taken", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onError(Exception e) {
                showProgress(false);
                Toast.makeText(ProfileActivity.this, "Check failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performUsernameUpdate(String newUsername) {
        String uid = UserSession.get().getUserId();
        issueRepo.setUsername(uid, newUsername, new Callback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                showProgress(false);
                UserSession.get().setUsername(newUsername);
                getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
                        .edit().putString(AppConstants.PREF_USERNAME, newUsername).apply();
                
                populateHeaderFromSession();
                Toast.makeText(ProfileActivity.this, "Username updated!", Toast.LENGTH_SHORT).show();
                
                // Refresh leaderboard to show new name
                loadLeaderboard();
            }
            @Override
            public void onError(Exception e) {
                showProgress(false);
                Toast.makeText(ProfileActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
