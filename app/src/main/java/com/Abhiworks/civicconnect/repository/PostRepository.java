package com.Abhiworks.civicconnect.repository;

import com.Abhiworks.civicconnect.models.CommunityPost;
import com.Abhiworks.civicconnect.utils.Callback;

import java.util.List;

public interface PostRepository {

    /** Fetch the community feed, newest first, limit 30. */
    void getPosts(Callback<List<CommunityPost>> callback);

    /** Submit a new community post. */
    void submitPost(String userId, String authorUsername,
                    String body, String imageUrl,
                    Callback<Void> callback);

    /** Check if a user has already upvoted a specific post. */
    void hasUpvoted(String userId, String postId, Callback<Boolean> callback);

    /** Call the increment_upvotes RPC. */
    void upvotePost(String postId, String userId, Callback<Void> callback);

    /** Upload a community image and return its public URL. */
    void uploadPostImage(android.net.Uri imageUri, String userId, Callback<String> callback);
}
