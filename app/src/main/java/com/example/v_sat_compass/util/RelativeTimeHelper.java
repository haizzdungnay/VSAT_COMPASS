package com.example.v_sat_compass.util;

import android.content.Context;

import com.example.v_sat_compass.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Định dạng thời gian dạng "vừa xong", "X phút trước", v.v.
 * Dùng string resources để dễ i18n sau này.
 */
public final class RelativeTimeHelper {

    private static final long MINUTE_MS = 60_000L;
    private static final long HOUR_MS   = 60 * MINUTE_MS;
    private static final long DAY_MS    = 24 * HOUR_MS;
    private static final long WEEK_MS   = 7 * DAY_MS;

    private static final SimpleDateFormat ABS_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi-VN"));

    private RelativeTimeHelper() {}

    /**
     * Trả về chuỗi thời gian tương đối so với hiện tại.
     * < 1 phút   → "vừa xong"
     * < 1 giờ    → "X phút trước"
     * < 24 giờ   → "X giờ trước"
     * < 7 ngày   → "X ngày trước"
     * còn lại    → "dd/MM/yyyy HH:mm"
     */
    public static String format(Context context, long timestampMillis) {
        long diff = System.currentTimeMillis() - timestampMillis;
        if (diff < 0) diff = 0;

        if (diff < MINUTE_MS) {
            return context.getString(R.string.time_just_now);
        } else if (diff < HOUR_MS) {
            long mins = diff / MINUTE_MS;
            return context.getString(R.string.time_minutes_ago, (int) mins);
        } else if (diff < DAY_MS) {
            long hours = diff / HOUR_MS;
            return context.getString(R.string.time_hours_ago, (int) hours);
        } else if (diff < WEEK_MS) {
            long days = diff / DAY_MS;
            return context.getString(R.string.time_days_ago, (int) days);
        } else {
            return ABS_FORMAT.format(new Date(timestampMillis));
        }
    }
}
