package com.petchat.messenger.ui.common.utils;

import android.content.Context;

import com.petchat.messenger.R;

import java.time.Instant;

public class PresenceFormatter {
    public static String format(Context context, boolean isOnline, Instant lastSeen) {
        if (isOnline) return context.getString(R.string.user_online);
        if (lastSeen == null) return context.getString(R.string.user_offline);

        return context.getString(R.string.user_last_seen, formatTime(lastSeen));
    }

    private static String formatTime(Instant lastSeen) {
        long lastSeenMilli = lastSeen.toEpochMilli(),
                now = System.currentTimeMillis();
        long diffMinutes = (now - lastSeenMilli) / 60_000;

        if (diffMinutes < 1) return "только что";
        if (diffMinutes < 60) return diffMinutes + " мин. назад";

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(lastSeenMilli));
    }
}