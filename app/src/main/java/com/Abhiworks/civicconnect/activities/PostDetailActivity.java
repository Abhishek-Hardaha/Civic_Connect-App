package com.Abhiworks.civicconnect.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.Abhiworks.civicconnect.R;
import com.Abhiworks.civicconnect.models.CommunityPost;
import com.Abhiworks.civicconnect.repository.SupabasePostRepository;
import com.Abhiworks.civicconnect.session.UserSession;
import com.Abhiworks.civicconnect.utils.AppConstants;
import com.Abhiworks.civicconnect.utils.Callback;
import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PostDetailActivity extends AppCompatActivity {

    private CommunityPost post;
    private SupabasePostRepository postRepo;
    private TextView btnUpvote;

    private static final int[] AVATAR_COLORS = {
            0xFF00E5FF, 0xFFBB86FC, 0xFFFFB300,
            0xFF00E676, 0xFFFF5252, 0xFF40C4FF
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        post = (CommunityPost) getIntent().getSerializableExtra(AppConstants.EXTRA_POST);
        if (post == null) {
            finish();
            return;
        }

        postRepo = new SupabasePostRepository(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        populate();
    }

    private void populate() {
        ImageView ivImage = findViewById(R.id.iv_post_image);
        TextView tvAvatar = findViewById(R.id.tv_avatar);
        TextView tvUsername = findViewById(R.id.tv_username);
        TextView tvTimestamp = findViewById(R.id.tv_timestamp);
        TextView tvBody = findViewById(R.id.tv_body);
        btnUpvote = findViewById(R.id.btn_upvote);

        // Image
        if (post.hasImage()) {
            ivImage.setVisibility(View.VISIBLE);
            Glide.with(this).load(post.getImageUrl()).into(ivImage);
        }

        // Author
        String username = post.getAuthorUsername();
        String initial = (username != null && !username.isEmpty()) ? String.valueOf(username.charAt(0)).toUpperCase() : "?";
        int avatarColor = AVATAR_COLORS[Math.abs((username != null ? username.hashCode() : 0)) % AVATAR_COLORS.length];
        tvAvatar.setText(initial);
        tvAvatar.getBackground().setTint(avatarColor);
        tvUsername.setText(username);
        tvTimestamp.setText(formatDate(post.getCreatedAt()));

        // Body
        tvBody.setText(post.getBody());

        // Interaction
        updateUpvoteUI();
        btnUpvote.setOnClickListener(v -> handleUpvote());
    }

    private void handleUpvote() {
        if (post.isVotedByCurrentUser()) {
            Toast.makeText(this, "Already upvoted", Toast.LENGTH_SHORT).show();
            return;
        }

        post.setVotedByCurrentUser(true);
        post.incrementUpvotes();
        updateUpvoteUI();

        postRepo.upvotePost(post.getId(), UserSession.get().getUserId(), new Callback<Void>() {
            @Override public void onSuccess(Void unused) {}
            @Override public void onError(Exception e) {
                post.setVotedByCurrentUser(false);
                post.decrementUpvotes();
                updateUpvoteUI();
                Snackbar.make(btnUpvote, "Upvote failed", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUpvoteUI() {
        btnUpvote.setText("▲  " + post.getUpvotes());
        if (post.isVotedByCurrentUser()) {
            btnUpvote.setTextColor(Color.parseColor("#00E5FF"));
        } else {
            btnUpvote.setTextColor(Color.parseColor("#66FFFFFF"));
        }
    }

    private String formatDate(String iso) {
        if (iso == null) return "";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            SimpleDateFormat out = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.US);
            Date d = in.parse(iso.length() > 19 ? iso.substring(0, 19) : iso);
            return d != null ? out.format(d) : iso;
        } catch (ParseException e) { return iso; }
    }
}
