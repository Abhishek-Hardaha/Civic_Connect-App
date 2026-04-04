package com.Abhiworks.civicconnect.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.Abhiworks.civicconnect.R;
import com.Abhiworks.civicconnect.repository.SupabaseIssueRepository;
import com.Abhiworks.civicconnect.session.UserSession;
import com.Abhiworks.civicconnect.utils.AppConstants;
import com.Abhiworks.civicconnect.utils.Callback;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Pattern;

public class SetUsernameActivity extends AppCompatActivity {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final long DEBOUNCE_MS = 600;

    private TextInputLayout tilUsername;
    private TextInputEditText etUsername;
    private TextView tvAvailability;
    private ProgressBar progress;
    private SupabaseIssueRepository issueRepo;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;
    private boolean isAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_username);
        // Override back — user cannot leave this screen
        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override public void handleOnBackPressed() { /* do nothing */ }
                });

        issueRepo      = new SupabaseIssueRepository(this);
        tilUsername    = findViewById(R.id.til_username);
        etUsername     = findViewById(R.id.et_username);
        tvAvailability = findViewById(R.id.tv_availability);
        progress       = findViewById(R.id.progress);

        etUsername.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                isAvailable = false;
                tvAvailability.setVisibility(View.INVISIBLE);
                debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> checkAvailability(s.toString().trim());
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_MS);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btn_get_started).setOnClickListener(v -> attemptSetUsername());
    }

    private void checkAvailability(String username) {
        if (!USERNAME_PATTERN.matcher(username).matches()) return;
        issueRepo.checkUsernameAvailable(username, new Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean available) {
                isAvailable = available;
                tvAvailability.setVisibility(View.VISIBLE);
                if (available) {
                    tvAvailability.setText("✓ Available");
                    tvAvailability.setTextColor(getColor(R.color.status_resolved));
                } else {
                    tvAvailability.setText("✗ Taken");
                    tvAvailability.setTextColor(getColor(R.color.status_rejected));
                }
            }
            @Override public void onError(Exception e) {
                tvAvailability.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void attemptSetUsername() {
        String username = etUsername.getText() != null
                          ? etUsername.getText().toString().trim() : "";
        tilUsername.setError(null);

        if (username.isEmpty()) {
            tilUsername.setError(getString(R.string.error_username_empty)); return;
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            tilUsername.setError(getString(R.string.error_username_format)); return;
        }
        if (!isAvailable) {
            tilUsername.setError("Username is taken or not yet verified"); return;
        }

        showLoader(true);
        issueRepo.setUsername(UserSession.get().getUserId(), username, new Callback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                showLoader(false);
                UserSession.get().setUsername(username);
                SharedPreferences prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putString(AppConstants.PREF_USERNAME, username).apply();
                startActivity(new Intent(SetUsernameActivity.this, HomeActivity.class));
                finish();
            }
            @Override
            public void onError(Exception e) {
                showLoader(false);
                Snackbar.make(tilUsername, getString(R.string.error_generic), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void showLoader(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
