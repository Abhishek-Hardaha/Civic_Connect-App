package com.Abhiworks.civicconnect.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.content.pm.PackageManager;

import com.Abhiworks.civicconnect.R;
import com.Abhiworks.civicconnect.repository.SupabasePostRepository;
import com.Abhiworks.civicconnect.session.UserSession;
import com.Abhiworks.civicconnect.utils.AppConstants;
import com.Abhiworks.civicconnect.utils.AuthException;
import com.Abhiworks.civicconnect.utils.Callback;
import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;

public class AddPostActivity extends AppCompatActivity {

    private TextInputLayout tilBody;
    private TextInputEditText etBody;
    private FrameLayout cardPhoto;
    private ImageView ivPhoto;
    private LinearLayout layoutAddPhoto;
    private View btnRemovePhoto, progress;
    private SupabasePostRepository postRepo;

    private Uri selectedImageUri, cameraImageUri;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        postRepo = new SupabasePostRepository(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tilBody       = findViewById(R.id.til_body);
        etBody        = findViewById(R.id.et_body);
        cardPhoto     = findViewById(R.id.card_photo);
        ivPhoto       = findViewById(R.id.iv_photo);
        layoutAddPhoto= findViewById(R.id.layout_add_photo);
        btnRemovePhoto= findViewById(R.id.btn_remove_photo);
        progress      = findViewById(R.id.progress);

        cardPhoto.setOnClickListener(v -> showImageSourceDialog());
        btnRemovePhoto.setOnClickListener(v -> clearPhoto());
        findViewById(R.id.btn_post).setOnClickListener(v -> submitPost());

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
                    if (isGranted) openCameraAction();
                    else showSnackbar("Camera permission is required");
                });
    }

    private void showImageSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Add Photo")
                .setItems(new String[]{"📷 Camera", "🖼 Gallery"}, (d, which) -> {
                    if (which == 0) checkCameraPermissionAndOpen(); else openGallery();
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
            File f = File.createTempFile("post_img_", ".jpg", cacheDir);
            cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            cameraLauncher.launch(cameraImageUri);
        } catch (IOException e) {
            showSnackbar("Could not prepare image file");
        }
    }

    private void openGallery() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(i);
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
                .withAspectRatio(1, 1) // Post images are 1:1
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

    private void submitPost() {
        String body = etBody.getText() != null ? etBody.getText().toString().trim() : "";
        tilBody.setError(null);

        if (TextUtils.isEmpty(body)) { tilBody.setError(getString(R.string.error_post_empty)); return; }

        showLoader(true);
        String uid      = UserSession.get().getUserId();
        String username = UserSession.get().getUsername();

        if (selectedImageUri != null) {
            postRepo.uploadPostImage(selectedImageUri, uid, new Callback<String>() {
                @Override
                public void onSuccess(String imageUrl) {
                    doSubmit(uid, username, body, imageUrl);
                }
                @Override
                public void onError(Exception e) {
                    showLoader(false);
                    showSnackbar("Image upload failed");
                }
            });
        } else {
            doSubmit(uid, username, body, null);
        }
    }

    private void doSubmit(String uid, String username, String body, String imageUrl) {
        postRepo.submitPost(uid, username, body, imageUrl, new Callback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                showLoader(false);
                finish(); // CommunityActivity reloads on resume
            }
            @Override
            public void onError(Exception e) {
                showLoader(false);
                if (e instanceof AuthException) {
                    UserSession.get().clear(getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE));
                    startActivity(new Intent(AddPostActivity.this, LoginActivity.class));
                    finishAffinity();
                } else {
                    showSnackbar(getString(R.string.error_generic));
                }
            }
        });
    }

    private void showLoader(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_post).setEnabled(!show);
    }

    private void showSnackbar(String msg) {
        Snackbar.make(tilBody, msg, Snackbar.LENGTH_LONG).show();
    }
}
