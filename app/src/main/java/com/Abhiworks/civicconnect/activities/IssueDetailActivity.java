package com.Abhiworks.civicconnect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.Abhiworks.civicconnect.R;
import com.Abhiworks.civicconnect.models.Issue;
import com.Abhiworks.civicconnect.repository.SupabaseIssueRepository;
import com.Abhiworks.civicconnect.session.UserSession;
import com.Abhiworks.civicconnect.ui.StatusBadgeHelper;
import com.Abhiworks.civicconnect.utils.AppConstants;
import com.Abhiworks.civicconnect.utils.AuthException;
import com.Abhiworks.civicconnect.utils.Callback;
import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.cardview.widget.CardView;

public class IssueDetailActivity extends AppCompatActivity {

    private ImageView ivIssueImage;
    private TextView tvCategory, tvStatusBadge, tvTitle, tvDescription, tvDate, tvLocality;
    private CardView cardResolution;
    private TextView tvResolvedAt, tvResolutionNote;
    private ImageView ivResolutionPhoto;
    private SupabaseIssueRepository issueRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_issue_detail);

        issueRepo = new SupabaseIssueRepository(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ivIssueImage     = findViewById(R.id.iv_issue_image);
        tvCategory       = findViewById(R.id.tv_category);
        tvStatusBadge    = findViewById(R.id.tv_status_badge);
        tvTitle          = findViewById(R.id.tv_title);
        tvDescription    = findViewById(R.id.tv_description);
        tvDate           = findViewById(R.id.tv_date);
        tvLocality       = findViewById(R.id.tv_locality);
        cardResolution   = findViewById(R.id.card_resolution);
        tvResolvedAt     = findViewById(R.id.tv_resolved_at);
        tvResolutionNote = findViewById(R.id.tv_resolution_note);
        ivResolutionPhoto= findViewById(R.id.iv_resolution_photo);

        String issueId = getIntent().getStringExtra(AppConstants.EXTRA_ISSUE_ID);
        if (issueId != null) {
            loadIssue(issueId);
        } else {
            finish();
        }
    }

    private void loadIssue(String id) {
        issueRepo.getIssueById(id, new Callback<Issue>() {
            @Override
            public void onSuccess(Issue issue) {
                populate(issue);
            }
            @Override
            public void onError(Exception e) {
                if (e instanceof AuthException) clearSessionAndGoLogin();
                else Snackbar.make(tvTitle, getString(R.string.error_generic), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void populate(Issue issue) {
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(issue.getTitle());

        // Issue image
        if (issue.hasImage()) {
            ivIssueImage.setVisibility(View.VISIBLE);
            Glide.with(this).load(issue.getImageUrl()).centerCrop().into(ivIssueImage);
        }

        tvCategory.setText(issue.getCategory());
        StatusBadgeHelper.apply(this, tvStatusBadge, issue.getStatus());
        tvTitle.setText(issue.getTitle());

        if (issue.getDescription() != null && !issue.getDescription().isEmpty()) {
            tvDescription.setVisibility(View.VISIBLE);
            tvDescription.setText(issue.getDescription());
        } else {
            tvDescription.setVisibility(View.GONE);
        }

        tvDate.setText("Filed on " + formatDate(issue.getCreatedAt()));

        if (issue.getLocality() != null && !issue.getLocality().isEmpty()) {
            tvLocality.setVisibility(View.VISIBLE);
            tvLocality.setText("📍 " + issue.getLocality());
        }

        // Resolution section — only when resolved
        if (issue.isResolved()) {
            cardResolution.setVisibility(View.VISIBLE);
            tvResolvedAt.setText("Resolved on " + formatDate(issue.getResolvedAt()));

            if (issue.hasResolutionNote()) {
                tvResolutionNote.setVisibility(View.VISIBLE);
                tvResolutionNote.setText(issue.getResolutionNote());
            }

            if (issue.hasResolutionPhoto()) {
                ivResolutionPhoto.setVisibility(View.VISIBLE);
                Glide.with(this)
                     .load(issue.getResolutionPhotoUrl())
                     .centerCrop()
                     .into(ivResolutionPhoto);
            }
        } else {
            cardResolution.setVisibility(View.GONE);
        }
    }

    private String formatDate(String iso) {
        if (iso == null) return "";
        try {
            SimpleDateFormat in  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            SimpleDateFormat out = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            Date d = in.parse(iso.length() > 19 ? iso.substring(0, 19) : iso);
            return d != null ? out.format(d) : iso;
        } catch (ParseException e) { return iso; }
    }

    private void clearSessionAndGoLogin() {
        UserSession.get().clear(getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE));
        startActivity(new Intent(this, LoginActivity.class));
        finishAffinity();
    }
}
