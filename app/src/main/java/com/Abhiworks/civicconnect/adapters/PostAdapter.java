package com.Abhiworks.civicconnect.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.Abhiworks.civicconnect.R;
import com.Abhiworks.civicconnect.models.CommunityPost;
import com.bumptech.glide.Glide;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.VH> {

    public interface OnUpvoteClickListener {
        void onUpvote(CommunityPost post, int position);
    }

    private List<CommunityPost> items = new ArrayList<>();
    private OnUpvoteClickListener upvoteListener;

    // Palette for avatar backgrounds (hashed from username)
    private static final int[] AVATAR_COLORS = {
            0xFF00E5FF, 0xFFBB86FC, 0xFFFFB300,
            0xFF00E676, 0xFFFF5252, 0xFF40C4FF
    };

    public PostAdapter(OnUpvoteClickListener listener) {
        this.upvoteListener = listener;
    }

    public void submitList(List<CommunityPost> newList) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize()                      { return items.size(); }
            @Override public int getNewListSize()                      { return newList.size(); }
            @Override public boolean areItemsTheSame(int o, int n)    { return items.get(o).getId().equals(newList.get(n).getId()); }
            @Override public boolean areContentsTheSame(int o, int n) { return items.get(o).getUpvotes() == newList.get(n).getUpvotes(); }
        });
        items = new ArrayList<>(newList);
        result.dispatchUpdatesTo(this);
    }

    public List<CommunityPost> getItems() { return items; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        CommunityPost post = items.get(position);
        Context ctx = h.itemView.getContext();

        // Avatar
        String username = post.getAuthorUsername();
        String initial  = (username != null && !username.isEmpty())
                          ? String.valueOf(username.charAt(0)).toUpperCase()
                          : "?";
        int avatarColor = AVATAR_COLORS[Math.abs((username != null ? username.hashCode() : 0))
                          % AVATAR_COLORS.length];
        h.tvAvatar.setText(initial);
        h.tvAvatar.getBackground().setTint(avatarColor);
        h.tvUsername.setText(username);
        h.tvTimestamp.setText(getRelativeTime(post.getCreatedAt()));

        // Body
        h.tvBody.setText(post.getBody());

        // Image
        if (post.hasImage()) {
            h.cardImage.setVisibility(View.VISIBLE);
            Glide.with(ctx)
                 .load(post.getImageUrl())
                 .centerCrop()
                 .placeholder(R.drawable.bg_placeholder)
                 .error(R.drawable.bg_placeholder)
                 .into(h.ivImage);
        } else {
            h.cardImage.setVisibility(View.GONE);
        }

        // Upvote button
        updateUpvoteButton(h.btnUpvote, post);
        h.btnUpvote.setOnClickListener(v -> upvoteListener.onUpvote(post, h.getAdapterPosition()));
    }

    @Override
    public int getItemCount() { return items.size(); }

    public void notifyItemUpvoteChanged(int position) {
        notifyItemChanged(position);
    }

    private void updateUpvoteButton(TextView btn, CommunityPost post) {
        btn.setText("▲  " + post.getUpvotes());
        if (post.isVotedByCurrentUser()) {
            btn.setTextColor(Color.parseColor("#00E5FF")); // cyan
        } else {
            btn.setTextColor(Color.parseColor("#66FFFFFF")); // dim
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView  tvAvatar, tvUsername, tvTimestamp, tvBody, btnUpvote;
        ImageView ivImage;
        View cardImage;

        VH(View v) {
            super(v);
            tvAvatar    = v.findViewById(R.id.tv_avatar);
            tvUsername  = v.findViewById(R.id.tv_username);
            tvTimestamp = v.findViewById(R.id.tv_timestamp);
            tvBody      = v.findViewById(R.id.tv_body);
            ivImage     = v.findViewById(R.id.iv_post_image);
            cardImage   = v.findViewById(R.id.card_post_image);
            btnUpvote   = v.findViewById(R.id.btn_upvote);
        }
    }

    private String getRelativeTime(String isoDate) {
        if (isoDate == null) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date d = sdf.parse(isoDate.length() > 19 ? isoDate.substring(0, 19) : isoDate);
            if (d == null) return isoDate;
            long diffMs = System.currentTimeMillis() - d.getTime();
            long hours = TimeUnit.MILLISECONDS.toHours(diffMs);
            if (hours < 1)  return TimeUnit.MILLISECONDS.toMinutes(diffMs) + "m ago";
            if (hours < 24) return hours + "h ago";
            long days = TimeUnit.MILLISECONDS.toDays(diffMs);
            if (days < 7)   return days + "d ago";
            return new SimpleDateFormat("MMM d", Locale.US).format(d);
        } catch (ParseException e) {
            return isoDate;
        }
    }
}
