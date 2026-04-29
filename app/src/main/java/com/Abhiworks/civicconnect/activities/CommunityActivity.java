package com.Abhiworks.civicconnect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.Abhiworks.civicconnect.R;
import com.Abhiworks.civicconnect.adapters.PostAdapter;
import com.Abhiworks.civicconnect.models.CommunityPost;
import com.Abhiworks.civicconnect.repository.SupabasePostRepository;
import com.Abhiworks.civicconnect.session.UserSession;
import com.Abhiworks.civicconnect.utils.AppConstants;
import com.Abhiworks.civicconnect.utils.AuthException;
import com.Abhiworks.civicconnect.utils.Callback;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class CommunityActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutEmpty;
    private PostAdapter adapter;
    private SupabasePostRepository postRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        postRepo = new SupabasePostRepository(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recycler     = findViewById(R.id.recycler_posts);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        layoutEmpty  = findViewById(R.id.layout_empty);

        adapter = new PostAdapter(this::handleUpvote, this::onPostClicked);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        swipeRefresh.setColorSchemeColors(getColor(R.color.cyan));
        swipeRefresh.setOnRefreshListener(this::loadPosts);

        ExtendedFloatingActionButton fab = findViewById(R.id.fab_add_post);
        fab.setOnClickListener(v ->
                startActivity(new Intent(this, AddPostActivity.class)));

        loadPosts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPosts();
    }

    private void loadPosts() {
        swipeRefresh.setRefreshing(true);
        postRepo.getPosts(new Callback<List<CommunityPost>>() {
            @Override
            public void onSuccess(List<CommunityPost> posts) {
                swipeRefresh.setRefreshing(false);
                // Check upvote status for each post
                checkUpvoteStates(posts);
                adapter.submitList(posts);
                layoutEmpty.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onError(Exception e) {
                swipeRefresh.setRefreshing(false);
                handleError(e);
            }
        });
    }

    /** Checks upvote status for the first N posts (performance optimization). */
    private void checkUpvoteStates(List<CommunityPost> posts) {
        String uid = UserSession.get().getUserId();
        for (CommunityPost post : posts) {
            postRepo.hasUpvoted(uid, post.getId(), new Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean voted) {
                    if (voted != post.isVotedByCurrentUser()) {
                        post.setVotedByCurrentUser(voted);
                        int idx = adapter.getItems().indexOf(post);
                        if (idx >= 0) adapter.notifyItemChanged(idx);
                    }
                }
                @Override public void onError(Exception e) {}
            });
        }
    }

    private void handleUpvote(CommunityPost post, int position) {
        if (post.isVotedByCurrentUser()) {
            Toast.makeText(this, "You've already upvoted this", Toast.LENGTH_SHORT).show();
            return;
        }

        // Optimistic update
        post.setVotedByCurrentUser(true);
        post.incrementUpvotes();
        adapter.notifyItemUpvoteChanged(position);

        postRepo.upvotePost(post.getId(), UserSession.get().getUserId(), new Callback<Void>() {
            @Override public void onSuccess(Void unused) { /* confirmed */ }
            @Override
            public void onError(Exception e) {
                // Revert optimistic update
                post.setVotedByCurrentUser(false);
                post.decrementUpvotes();
                adapter.notifyItemUpvoteChanged(position);
                Snackbar.make(recycler, "Upvote failed. Try again.", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void handleError(Exception e) {
        if (e instanceof AuthException) {
            UserSession.get().clear(getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE));
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        } else {
            Snackbar.make(recycler, getString(R.string.error_network), Snackbar.LENGTH_LONG)
                    .setAction("Retry", v -> loadPosts()).show();
        }
    }

    private void onPostClicked(CommunityPost post) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra(AppConstants.EXTRA_POST, post);
        startActivity(intent);
    }
}
