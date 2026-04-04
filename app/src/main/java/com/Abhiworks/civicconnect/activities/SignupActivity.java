package com.Abhiworks.civicconnect.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.Abhiworks.civicconnect.R;
import com.Abhiworks.civicconnect.service.SupabaseService;
import com.Abhiworks.civicconnect.session.UserSession;
import com.Abhiworks.civicconnect.utils.AppConstants;
import com.Abhiworks.civicconnect.utils.Callback;
import com.Abhiworks.civicconnect.utils.JsonParser;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class SignupActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etEmail, etPassword, etConfirmPassword;
    private ProgressBar progress;
    private SupabaseService supabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        supabase = SupabaseService.getInstance(this);

        tilEmail           = findViewById(R.id.til_email);
        tilPassword        = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        etEmail            = findViewById(R.id.et_email);
        etPassword         = findViewById(R.id.et_password);
        etConfirmPassword  = findViewById(R.id.et_confirm_password);
        progress           = findViewById(R.id.progress);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_create).setOnClickListener(v -> attemptSignup());
        findViewById(R.id.tv_sign_in).setOnClickListener(v -> finish());
    }

    private void attemptSignup() {
        String email    = text(etEmail);
        String password = text(etPassword);
        String confirm  = text(etConfirmPassword);
        boolean valid   = true;

        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

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
        if (!password.equals(confirm)) {
            tilConfirmPassword.setError(getString(R.string.error_passwords_dont_match)); valid = false;
        }
        if (!valid) return;

        showLoader(true);
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        String url  = supabase.authUrl + "/signup";

        supabase.postAuth(url, body, new Callback<String>() {
            @Override
            public void onSuccess(String json) {
                showLoader(false);
                String jwt     = JsonParser.parseAccessToken(json);
                String refresh = JsonParser.parseRefreshToken(json);
                String userId  = JsonParser.parseUserId(json);

                SharedPreferences prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);
                UserSession.get().init(jwt, refresh, userId, null);
                UserSession.get().saveToPrefs(prefs);

                // New users always go to username setup
                startActivity(new Intent(SignupActivity.this, SetUsernameActivity.class));
                finish();
            }

            @Override
            public void onError(Exception e) {
                showLoader(false);
                String msg = e.getMessage() != null && e.getMessage().contains("422")
                        ? "An account with this email already exists"
                        : getString(R.string.error_generic);
                Snackbar.make(tilEmail, msg, Snackbar.LENGTH_LONG).show();
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
