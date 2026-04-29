package com.Abhiworks.civicconnect.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.content.pm.PackageManager;

import com.Abhiworks.civicconnect.R;
import com.Abhiworks.civicconnect.models.Issue;
import com.Abhiworks.civicconnect.repository.SupabaseIssueRepository;
import com.Abhiworks.civicconnect.session.UserSession;
import com.Abhiworks.civicconnect.utils.AppConstants;
import com.Abhiworks.civicconnect.utils.AuthException;
import com.Abhiworks.civicconnect.utils.Callback;
import com.Abhiworks.civicconnect.utils.LocationUtils;
import com.Abhiworks.civicconnect.utils.NetworkException;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;

public class ReportIssueActivity extends AppCompatActivity {

    private TextInputLayout tilTitle, tilLocality, tilDescription;
    private TextInputEditText etTitle, etLocality, etDescription;
    private ChipGroup chipGroupCategory;
    private TextView tvCategoryError, tvLocationResult;
    private FrameLayout cardPhoto;
    private ImageView ivPhoto;
    private LinearLayout layoutAddPhoto;
    private TextView btnRemovePhoto;
    private View progress;

    private SupabaseIssueRepository issueRepo;
    private Uri selectedImageUri;
    private Uri cameraImageUri;
    private Double latitude, longitude;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);

        issueRepo = new SupabaseIssueRepository(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        tilTitle       = findViewById(R.id.til_title);
        tilLocality    = findViewById(R.id.til_locality);
        tilDescription = findViewById(R.id.til_description);
        etTitle        = findViewById(R.id.et_title);
        etLocality     = findViewById(R.id.et_locality);
        etDescription  = findViewById(R.id.et_description);
        chipGroupCategory = findViewById(R.id.chip_group_category);
        tvCategoryError   = findViewById(R.id.tv_category_error);
        tvLocationResult  = findViewById(R.id.tv_location_result);
        cardPhoto      = findViewById(R.id.card_photo);
        ivPhoto        = findViewById(R.id.iv_photo);
        layoutAddPhoto = findViewById(R.id.layout_add_photo);
        btnRemovePhoto = findViewById(R.id.btn_remove_photo);
        progress       = findViewById(R.id.progress);

        // Build category chips
        for (String cat : AppConstants.CATEGORIES) {
            Chip chip = new Chip(this);
            chip.setText(cat);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.bg_card);
            chip.setTextColor(getColor(R.color.text_secondary));
            chip.setCheckedIconVisible(false);
            chipGroupCategory.addView(chip);
        }

        // Photo card
        cardPhoto.setOnClickListener(v -> showImageSourceDialog());
        btnRemovePhoto.setOnClickListener(v -> clearPhoto());

        // Location
        findViewById(R.id.btn_location).setOnClickListener(v -> requestLocation());

        // Submit
        findViewById(R.id.btn_submit).setOnClickListener(v -> submit());

        // Activity result launchers
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        startCrop(result.getData().getData());
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraImageUri != null) {
                        startCrop(cameraImageUri);
                    }
                });

        cropLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = UCrop.getOutput(result.getData());
                        if (selectedImageUri != null) {
                            showSelectedImage(selectedImageUri);
                        }
                    } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                        Throwable cropError = UCrop.getError(result.getData());
                        showSnackbar("Crop error: " + (cropError != null ? cropError.getMessage() : "Unknown"));
                    }
                });

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCameraAction();
                    } else {
                        showSnackbar("Camera permission is required to take photos");
                    }
                });
    }

    private void showImageSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Add Photo")
                .setItems(new String[]{"📷 Camera", "🖼 Gallery"}, (dialog, which) -> {
                    if (which == 0) checkCameraPermissionAndOpen();
                    else            openGallery();
                })
                .show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCameraAction();
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }
    }

    private void openCameraAction() {
        try {
            File cacheDir = getExternalCacheDir();
            if (cacheDir == null) cacheDir = getCacheDir();
            
            File imgFile = File.createTempFile("img_", ".jpg", cacheDir);
            cameraImageUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", imgFile);
            cameraLauncher.launch(cameraImageUri);
        } catch (IOException e) {
            showSnackbar("Could not prepare image file: " + e.getMessage());
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void startCrop(Uri sourceUri) {
        Uri destinationUri = Uri.fromFile(new File(getExternalCacheDir(), "crop_" + System.currentTimeMillis() + ".jpg"));
        
        UCrop.Options options = new UCrop.Options();
        options.setCompressionQuality(80);
        options.setToolbarColor(getColor(R.color.bg_dark));
        options.setStatusBarColor(getColor(R.color.bg_dark));
        options.setActiveControlsWidgetColor(getColor(R.color.cyan));
        options.setToolbarWidgetColor(getColor(R.color.text_primary));
        options.setToolbarTitle("Crop Photo");

        Intent intent = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(16, 9)
                .withMaxResultSize(1080, 1080)
                .withOptions(options)
                .getIntent(this);
        
        cropLauncher.launch(intent);
    }

    private void showSelectedImage(Uri uri) {
        layoutAddPhoto.setVisibility(View.GONE);
        ivPhoto.setVisibility(View.VISIBLE);
        btnRemovePhoto.setVisibility(View.VISIBLE);
        Glide.with(this).load(uri).centerCrop().into(ivPhoto);
    }

    private void clearPhoto() {
        selectedImageUri = null;
        ivPhoto.setVisibility(View.GONE);
        btnRemovePhoto.setVisibility(View.GONE);
        layoutAddPhoto.setVisibility(View.VISIBLE);
    }

    private void requestLocation() {
        if (!LocationUtils.hasLocationPermission(this)) {
            LocationUtils.requestLocationPermission(this);
            return;
        }
        LocationUtils.getCurrentLocation(this, new Callback<Location>() {
            @Override
            public void onSuccess(Location loc) {
                latitude  = loc.getLatitude();
                longitude = loc.getLongitude();
                tvLocationResult.setVisibility(View.VISIBLE);
                tvLocationResult.setText(String.format("📍 %.5f, %.5f", latitude, longitude));
                
                // Automatically reverse geocode to get locality
                try {
                    Geocoder geocoder = new Geocoder(ReportIssueActivity.this, java.util.Locale.getDefault());
                    java.util.List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        String localityStr = addr.getLocality();
                        if (localityStr == null) localityStr = addr.getSubAdminArea();
                        if (localityStr == null) localityStr = addr.getAdminArea();
                        
                        if (localityStr != null && !localityStr.isEmpty()) {
                            etLocality.setText(localityStr);
                        }
                    }
                } catch (Exception e) {
                    // Ignore geocoder errors; user can still type manually
                }
            }
            @Override
            public void onError(Exception e) {
                showSnackbar("Could not get location. Try typing your locality.");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == LocationUtils.LOCATION_PERMISSION_REQUEST_CODE) {
            if (LocationUtils.hasLocationPermission(this)) {
                requestLocation();
            }
        }
    }

    private void submit() {
        String title       = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        int checkedId      = chipGroupCategory.getCheckedChipId();
        String locality    = etLocality.getText() != null ? etLocality.getText().toString().trim() : "";

        tilTitle.setError(null);
        tvCategoryError.setVisibility(View.GONE);
        boolean valid = true;

        if (title.isEmpty()) { tilTitle.setError(getString(R.string.error_title_empty)); valid = false; }
        if (checkedId == View.NO_ID) { tvCategoryError.setVisibility(View.VISIBLE); valid = false; }
        if (!valid) return;

        Chip selectedChip = chipGroupCategory.findViewById(checkedId);
        String category   = selectedChip.getText().toString();
        String userId     = UserSession.get().getUserId();

        showLoader(true);

        Issue.Builder builder = new Issue.Builder(title, category, userId)
                .description(description.isEmpty() ? null : description)
                .latitude(latitude)
                .longitude(longitude)
                .locality(locality.isEmpty() ? null : locality);

        if (selectedImageUri != null) {
            // Upload image first
            issueRepo.uploadIssueImage(selectedImageUri, userId, new Callback<String>() {
                @Override
                public void onSuccess(String imageUrl) {
                    builder.imageUrl(imageUrl);
                    submitIssue(builder);
                }
                @Override
                public void onError(Exception e) {
                    showLoader(false);
                    showSnackbar("Image upload failed. " + e.getMessage());
                }
            });
        } else {
            submitIssue(builder);
        }
    }

    private void submitIssue(Issue.Builder builder) {
        issueRepo.submitIssue(builder, new Callback<Issue>() {
            @Override
            public void onSuccess(Issue issue) {
                showLoader(false);
                Snackbar.make(tilTitle, getString(R.string.issue_submitted), Snackbar.LENGTH_LONG).show();
                // Clear form
                etTitle.setText("");
                etDescription.setText("");
                chipGroupCategory.clearCheck();
                etLocality.setText("");
                latitude = null; longitude = null;
                clearPhoto();
                tvLocationResult.setVisibility(View.GONE);
            }
            @Override
            public void onError(Exception e) {
                showLoader(false);
                handleError(e);
            }
        });
    }

    private void handleError(Exception e) {
        if (e instanceof AuthException) {
            clearSessionAndGoLogin();
        } else if (e instanceof NetworkException) {
            Snackbar.make(tilTitle, getString(R.string.error_network), Snackbar.LENGTH_LONG)
                    .setAction("Retry", v -> submit()).show();
        } else {
            showSnackbar(getString(R.string.error_generic));
        }
    }

    private void clearSessionAndGoLogin() {
        UserSession.get().clear(getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE));
        startActivity(new Intent(this, LoginActivity.class));
        finishAffinity();
    }

    private void showLoader(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_submit).setEnabled(!show);
    }

    private void showSnackbar(String msg) {
        Snackbar.make(tilTitle, msg, Snackbar.LENGTH_LONG).show();
    }
}
