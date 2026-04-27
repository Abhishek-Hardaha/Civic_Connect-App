package com.Abhiworks.civicconnect.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.Abhiworks.civicconnect.R;
import com.Abhiworks.civicconnect.models.Issue;
import com.Abhiworks.civicconnect.ui.StatusBadgeHelper;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact adapter for the horizontal Recent Reports strip on HomeActivity.
 */
public class RecentIssueAdapter extends RecyclerView.Adapter<RecentIssueAdapter.VH> {

    public interface OnItemClickListener {
        void onIssueClick(Issue issue);
    }

    private final List<Issue> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public RecentIssueAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Issue> newList) {
        items.clear();
        if (newList != null) items.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_report, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Issue issue = items.get(position);
        Context ctx = h.itemView.getContext();

        h.tvTitle.setText(issue.getTitle());
        h.tvCategory.setText(issue.getCategory());
        StatusBadgeHelper.apply(ctx, h.tvStatus, issue.getStatus());

        if (issue.hasImage()) {
            Glide.with(ctx)
                 .load(issue.getImageUrl())
                 .centerCrop()
                 .placeholder(R.drawable.bg_placeholder)
                 .error(R.drawable.bg_placeholder)
                 .into(h.ivThumbnail);
        } else {
            h.ivThumbnail.setImageResource(R.drawable.bg_placeholder);
        }

        h.itemView.setOnClickListener(v -> listener.onIssueClick(issue));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView  tvTitle, tvCategory, tvStatus;

        VH(View v) {
            super(v);
            ivThumbnail = v.findViewById(R.id.iv_thumbnail);
            tvTitle     = v.findViewById(R.id.tv_title);
            tvCategory  = v.findViewById(R.id.tv_category);
            tvStatus    = v.findViewById(R.id.tv_status_badge);
        }
    }
}
