package com.Abhiworks.civicconnect.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.Abhiworks.civicconnect.R;
import com.Abhiworks.civicconnect.repository.SupabaseIssueRepository;
import com.Abhiworks.civicconnect.service.SupabaseService;
import com.Abhiworks.civicconnect.session.UserSession;
import com.Abhiworks.civicconnect.utils.AppConstants;
import com.Abhiworks.civicconnect.utils.Callback;
import com.Abhiworks.civicconnect.utils.JsonParser;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private ProgressBar progress;
    private SupabaseService supabase;
    private SupabaseIssueRepository issueRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        supabase  = SupabaseService.getInstance(this);
        issueRepo = new SupabaseIssueRepository(this);

        tilEmail    = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        etEmail     = findViewById(R.id.et_email);
        etPassword  = findViewById(R.id.et_password);
        progress    = findViewById(R.id.progress);

        findViewById(R.id.btn_sign_in).setOnClickListener(v -> attemptLogin());
        findViewById(R.id.tv_sign_up).setOnClickListener(v ->
                startActivity(new Intent(this, SignupActivity.class)));
    }

    private void attemptLogin() {
        String email    = text(etEmail);
        String password = text(etPassword);
        boolean valid = true;

        tilEmail.setError(null);
        tilPassword.setError(null);

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_email_empty)); valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_email_invalid)); valid = false;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.error_password_empty)); valid = false;
        } else if (password.length() < 6) {
            tilPassword.setError(getString(R.string.error_password_short)); valid = false;
        }
        if (!valid) return;

        showLoader(true);
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        String url  = supabase.authUrl + "/token?grant_type=password";

        supabase.postAuth(url, body, new Callback<String>() {
            @Override
            public void onSuccess(String json) {
                String jwt     = JsonParser.parseAccessToken(json);
                String refresh = JsonParser.parseRefreshToken(json);
                String userId  = JsonParser.parseUserId(json);

                SharedPreferences prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);
                UserSession.get().init(jwt, refresh, userId, null, null);
                UserSession.get().saveToPrefs(prefs);

                // Fetch profile to check username
                issueRepo.getProfile(userId, new Callback<com.Abhiworks.civicconnect.models.UserProfile>() {
                    @Override
                    public void onSuccess(com.Abhiworks.civicconnect.models.UserProfile profile) {
                        showLoader(false);
                        String username = profile != null ? profile.getUsername() : null;
                        String city     = profile != null ? profile.getCity()     : null;
                        UserSession.get().setUsername(username);
                        UserSession.get().setCity(city);
                        prefs.edit()
                             .putString(AppConstants.PREF_USERNAME, username)
                             .putString(AppConstants.PREF_CITY, city)
                             .apply();

                        if (TextUtils.isEmpty(username)) {
                            startActivity(new Intent(LoginActivity.this, SetUsernameActivity.class));
                        } else {
                            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                        }
                        finish();
                    }
                    @Override
                    public void onError(Exception e) {
                        showLoader(false);
                        // Profile fetch failed but auth succeeded — go home anyway
                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                        finish();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                showLoader(false);
                Snackbar.make(tilEmail, getString(R.string.error_login), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void showLoader(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
