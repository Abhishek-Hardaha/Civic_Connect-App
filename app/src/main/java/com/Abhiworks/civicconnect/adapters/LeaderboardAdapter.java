package com.Abhiworks.civicconnect.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.Abhiworks.civicconnect.R;
import com.Abhiworks.civicconnect.models.LeaderboardEntry;

import java.util.List;

/**
 * RecyclerView adapter for the city leaderboard in ProfileActivity.
 * - Top 3 rows get medal emojis (🥇 🥈 🥉).
 * - The row matching the logged-in user's username gets a highlighted background.
 */
public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.VH> {

    private final List<LeaderboardEntry> entries;
    private final String currentUsername;

    public LeaderboardAdapter(List<LeaderboardEntry> entries, String currentUsername) {
        this.entries         = entries;
        this.currentUsername = currentUsername;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        LeaderboardEntry entry = entries.get(position);
        Context ctx = h.itemView.getContext();

        // Rank badge — medal emoji for top 3, number otherwise
        String rankLabel;
        switch (entry.getRank()) {
            case 1:  rankLabel = "🥇"; break;
            case 2:  rankLabel = "🥈"; break;
            case 3:  rankLabel = "🥉"; break;
            default: rankLabel = "#" + entry.getRank();
        }
        h.tvRank.setText(rankLabel);
        
        String name = entry.getUsername();
        if (name != null && !name.isEmpty()) {
            h.tvUsername.setText("@" + name);
        } else {
            h.tvUsername.setText("Citizen #" + entry.getRank());
            h.tvUsername.setAlpha(0.6f); // De-emphasize anonymous users
        }

        h.tvReports.setText(String.valueOf(entry.getReportsRaised()));
        h.tvUpvotes.setText(String.valueOf(entry.getTotalUpvotes()));

        // Highlight current user's row
        boolean isMe = entry.getUsername() != null
                && entry.getUsername().equals(currentUsername);
        if (isMe) {
            h.itemView.setBackgroundResource(R.drawable.bg_leaderboard_me);
            h.tvUsername.setTextColor(ctx.getColor(R.color.cyan));
            h.tvRank.setTextColor(ctx.getColor(R.color.cyan));
        } else {
            h.itemView.setBackgroundResource(R.drawable.bg_leaderboard_row);
            h.tvUsername.setTextColor(ctx.getColor(R.color.text_primary));
            h.tvRank.setTextColor(ctx.getColor(R.color.text_secondary));
        }
    }

    @Override
    public int getItemCount() { return entries.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvRank, tvUsername, tvReports, tvUpvotes;
        VH(@NonNull View v) {
            super(v);
            tvRank     = v.findViewById(R.id.tv_rank);
            tvUsername = v.findViewById(R.id.tv_username);
            tvReports  = v.findViewById(R.id.tv_reports);
            tvUpvotes  = v.findViewById(R.id.tv_upvotes);
        }
    }
}
