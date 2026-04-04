package com.Abhiworks.civicconnect.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.Abhiworks.civicconnect.R;
import com.Abhiworks.civicconnect.session.UserSession;
import com.Abhiworks.civicconnect.utils.AppConstants;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialise session from disk — this is the one place that reads SharedPreferences
        SharedPreferences prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);
        UserSession.get().loadFromPrefs(prefs);

        new Handler(Looper.getMainLooper()).postDelayed(this::route, SPLASH_DELAY_MS);
    }

    private void route() {
        Intent intent;
        if (UserSession.get().isLoggedIn()) {
            if (UserSession.get().hasUsername()) {
                intent = new Intent(this, HomeActivity.class);
            } else {
                intent = new Intent(this, SetUsernameActivity.class);
            }
        } else {
            intent = new Intent(this, LoginActivity.class);
        }
        startActivity(intent);
        finish(); // Never allow back-press return to splash
    }
}
