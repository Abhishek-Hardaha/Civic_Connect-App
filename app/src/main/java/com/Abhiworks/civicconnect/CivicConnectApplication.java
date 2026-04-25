package com.Abhiworks.civicconnect;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class CivicConnectApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Apply saved theme before any Activity is created to prevent flash
        applyTheme();
    }

    private void applyTheme() {
        SharedPreferences prefs = getSharedPreferences(
                com.Abhiworks.civicconnect.utils.AppConstants.PREFS_NAME, MODE_PRIVATE);
        String themeMode = prefs.getString(
                com.Abhiworks.civicconnect.utils.AppConstants.PREF_THEME_MODE, "system");
        switch (themeMode) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
