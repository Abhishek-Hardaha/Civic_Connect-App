package com.Abhiworks.civicconnect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.Abhiworks.civicconnect.R;
import com.Abhiworks.civicconnect.adapters.IssueAdapter;
import com.Abhiworks.civicconnect.models.Issue;
import com.Abhiworks.civicconnect.repository.SupabaseIssueRepository;
import com.Abhiworks.civicconnect.session.UserSession;
import com.Abhiworks.civicconnect.utils.AppConstants;
import com.Abhiworks.civicconnect.utils.AuthException;
import com.Abhiworks.civicconnect.utils.Callback;
import com.Abhiworks.civicconnect.utils.NetworkException;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MyReportsActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutEmpty;
    private ChipGroup chipGroupFilter;
    private IssueAdapter adapter;
    private SupabaseIssueRepository issueRepo;
    private List<Issue> allIssues = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reports);

        issueRepo = new SupabaseIssueRepository(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recycler        = findViewById(R.id.recycler_issues);
        swipeRefresh    = findViewById(R.id.swipe_refresh);
        layoutEmpty     = findViewById(R.id.layout_empty);
        chipGroupFilter = findViewById(R.id.chip_group_filter);

        adapter = new IssueAdapter(issue -> {
            Intent intent = new Intent(this, IssueDetailActivity.class);
            intent.putExtra(AppConstants.EXTRA_ISSUE_ID, issue.getId());
            startActivity(intent);
        });
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> applyFilter());

        swipeRefresh.setOnRefreshListener(this::loadIssues);
        swipeRefresh.setColorSchemeColors(getColor(R.color.cyan));

        loadIssues();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadIssues();
    }

    private void loadIssues() {
        swipeRefresh.setRefreshing(true);
        issueRepo.getMyIssues(UserSession.get().getUserId(), new Callback<List<Issue>>() {
            @Override
            public void onSuccess(List<Issue> issues) {
                swipeRefresh.setRefreshing(false);
                allIssues = issues;
                applyFilter();
            }
            @Override
            public void onError(Exception e) {
                swipeRefresh.setRefreshing(false);
                handleError(e);
            }
        });
    }

    private void applyFilter() {
        int checkedId = chipGroupFilter.getCheckedChipId();
        List<Issue> filtered;

        if (checkedId == R.id.chip_pending) {
            filtered = filter(AppConstants.STATUS_PENDING);
        } else if (checkedId == R.id.chip_in_progress) {
            filtered = filter(AppConstants.STATUS_IN_PROGRESS);
        } else if (checkedId == R.id.chip_resolved) {
            filtered = filter(AppConstants.STATUS_RESOLVED);
        } else if (checkedId == R.id.chip_rejected) {
            filtered = filter(AppConstants.STATUS_REJECTED);
        } else {
            filtered = new ArrayList<>(allIssues);
        }

        adapter.submitList(filtered);
        layoutEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private List<Issue> filter(String status) {
        return allIssues.stream()
                .filter(i -> status.equals(i.getStatus()))
                .collect(Collectors.toList());
    }

    private void handleError(Exception e) {
        if (e instanceof AuthException) {
            clearSessionAndGoLogin();
        } else {
            Snackbar.make(recycler, getString(R.string.error_network), Snackbar.LENGTH_LONG)
                    .setAction("Retry", v -> loadIssues()).show();
        }
    }

    private void clearSessionAndGoLogin() {
        UserSession.get().clear(getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE));
        startActivity(new Intent(this, LoginActivity.class));
        finishAffinity();
    }
}
