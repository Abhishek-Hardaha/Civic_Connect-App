package com.Abhiworks.civicconnect.ui;

import android.content.Context;
import android.widget.TextView;

import com.Abhiworks.civicconnect.R;
import com.Abhiworks.civicconnect.utils.AppConstants;

/**
 * Applies the correct drawable background and text colour to a status badge TextView.
 */
public class StatusBadgeHelper {

    private StatusBadgeHelper() {}

    public static void apply(Context context, TextView badge, String status) {
        if (status == null) {
            badge.setText("UNKNOWN");
            badge.setBackgroundResource(R.drawable.bg_status_pending);
            badge.setTextColor(context.getColor(R.color.status_pending));
            return;
        }
        switch (status) {
            case AppConstants.STATUS_PENDING:
                badge.setText("PENDING");
                badge.setBackgroundResource(R.drawable.bg_status_pending);
                badge.setTextColor(context.getColor(R.color.status_pending));
                break;
            case AppConstants.STATUS_IN_PROGRESS:
                badge.setText("IN PROGRESS");
                badge.setBackgroundResource(R.drawable.bg_status_in_progress);
                badge.setTextColor(context.getColor(R.color.status_in_progress));
                break;
            case AppConstants.STATUS_RESOLVED:
                badge.setText("RESOLVED");
                badge.setBackgroundResource(R.drawable.bg_status_resolved);
                badge.setTextColor(context.getColor(R.color.status_resolved));
                break;
            case AppConstants.STATUS_REJECTED:
                badge.setText("REJECTED");
                badge.setBackgroundResource(R.drawable.bg_status_rejected);
                badge.setTextColor(context.getColor(R.color.status_rejected));
                break;
            default:
                badge.setText(status.toUpperCase());
                badge.setBackgroundResource(R.drawable.bg_status_pending);
                badge.setTextColor(context.getColor(R.color.status_pending));
                break;
        }
    }
}
