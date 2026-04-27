package com.Abhiworks.civicconnect.repository;

import com.Abhiworks.civicconnect.models.Issue;
import com.Abhiworks.civicconnect.models.LeaderboardEntry;
import com.Abhiworks.civicconnect.models.UserProfile;
import com.Abhiworks.civicconnect.utils.Callback;

import java.util.List;

public interface IssueRepository {

    /** Fetch all issues for the current user, newest first. */
    void getMyIssues(String userId, Callback<List<Issue>> callback);

    /** Fetch full details of a single issue by ID. */
    void getIssueById(String issueId, Callback<Issue> callback);

    /** Submit a new issue report (with optional image URL). */
    void submitIssue(Issue.Builder builder, Callback<Issue> callback);

    /** Fetch the profile for a given user ID. */
    void getProfile(String userId, Callback<UserProfile> callback);

    /** Check username availability. Returns true if available. */
    void checkUsernameAvailable(String username, Callback<Boolean> callback);

    /** Set the username on the user's profile (onboarding step). */
    void setUsername(String userId, String username, Callback<Void> callback);

    /** Fetch leaderboard entries ordered by reports_raised. City may be null for global. */
    void getLeaderboard(String city, Callback<List<LeaderboardEntry>> callback);

    /** Fetch most recent issues (up to limit) for the current user. */
    void getRecentIssues(String userId, int limit, Callback<List<Issue>> callback);
}
