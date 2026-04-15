package com.Abhiworks.civicconnect.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Location helper using FusedLocationProviderClient.
 * All callbacks are already on the main thread (LocationCallback is on main looper).
 */
public class LocationUtils {

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private LocationUtils() {}

    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    /**
     * Requests a single location fix and returns it via callback.
     * The callback is always invoked on the main thread.
     * Caller must verify permissions before calling this.
     */
    public static void getCurrentLocation(Activity activity, Callback<Location> callback) {
        if (!hasLocationPermission(activity)) {
            callback.onError(new SecurityException("Location permission not granted"));
            return;
        }

        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(activity);

        // Try last known location first (faster, no battery drain)
        try {
            client.getLastLocation().addOnSuccessListener(activity, location -> {
                if (location != null) {
                    callback.onSuccess(location);
                } else {
                    // Request a fresh fix
                    requestFreshLocation(client, activity, callback);
                }
            }).addOnFailureListener(e -> callback.onError(new NetworkException("Location unavailable: " + e.getMessage(), e)));
        } catch (SecurityException e) {
            callback.onError(e);
        }
    }

    private static void requestFreshLocation(FusedLocationProviderClient client,
                                              Activity activity,
                                              Callback<Location> callback) {
        LocationRequest req = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMaxUpdates(1)
                .build();

        LocationCallback lc = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                client.removeLocationUpdates(this);
                if (result != null && !result.getLocations().isEmpty()) {
                    callback.onSuccess(result.getLocations().get(0));
                } else {
                    callback.onError(new NetworkException("Could not get location"));
                }
            }
        };

        try {
            client.requestLocationUpdates(req, lc, Looper.getMainLooper());
        } catch (SecurityException e) {
            callback.onError(e);
        }
    }
}
