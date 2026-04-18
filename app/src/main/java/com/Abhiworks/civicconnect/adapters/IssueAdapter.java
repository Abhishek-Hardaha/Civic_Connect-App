package com.Abhiworks.civicconnect.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.Abhiworks.civicconnect.R;
import com.Abhiworks.civicconnect.models.Issue;
import com.Abhiworks.civicconnect.ui.StatusBadgeHelper;
import com.bumptech.glide.Glide;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IssueAdapter extends RecyclerView.Adapter<IssueAdapter.VH> {

    public interface OnItemClickListener {
        void onIssueClick(Issue issue);
    }

    private List<Issue> items = new ArrayList<>();
    private OnItemClickListener listener;

    public IssueAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Issue> newList) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize()                                  { return items.size(); }
            @Override public int getNewListSize()                                  { return newList.size(); }
            @Override public boolean areItemsTheSame(int o, int n)                { return items.get(o).getId().equals(newList.get(n).getId()); }
            @Override public boolean areContentsTheSame(int o, int n)             { return items.get(o).equals(newList.get(n)) && items.get(o).getStatus().equals(newList.get(n).getStatus()); }
        });
        items = new ArrayList<>(newList);
        result.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_issue, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Issue issue = items.get(position);
        Context ctx = h.itemView.getContext();

        h.tvTitle.setText(issue.getTitle());
        h.tvCategory.setText(issue.getCategory());
        StatusBadgeHelper.apply(ctx, h.tvStatus, issue.getStatus());
        h.tvDate.setText(formatDate(issue.getCreatedAt()));

        if (issue.hasImage()) {
            Glide.with(ctx)
                 .load(issue.getImageUrl())
                 .centerCrop()
                 .placeholder(R.drawable.bg_card_rounded)
                 .into(h.ivThumbnail);
        } else {
            // Show category emoji as placeholder
            h.ivThumbnail.setImageResource(R.drawable.bg_card_rounded);
        }

        h.itemView.setOnClickListener(v -> listener.onIssueClick(issue));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView  tvTitle, tvCategory, tvStatus, tvDate;

        VH(View v) {
            super(v);
            ivThumbnail = v.findViewById(R.id.iv_thumbnail);
            tvTitle     = v.findViewById(R.id.tv_title);
            tvCategory  = v.findViewById(R.id.tv_category);
            tvStatus    = v.findViewById(R.id.tv_status_badge);
            tvDate      = v.findViewById(R.id.tv_date);
        }
    }

    private String formatDate(String isoDate) {
        if (isoDate == null) return "";
        try {
            SimpleDateFormat sdf  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            SimpleDateFormat out  = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            Date d = sdf.parse(isoDate.length() > 19 ? isoDate.substring(0, 19) : isoDate);
            return d != null ? out.format(d) : isoDate;
        } catch (ParseException e) {
            return isoDate;
        }
    }
}
