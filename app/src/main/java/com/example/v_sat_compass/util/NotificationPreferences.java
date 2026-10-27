package com.example.v_sat_compass.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.v_sat_compass.VsatApp;

/**
 * Local settings for the F8 study reminder (WorkManager-driven, NOT FCM).
 * Keeps state on-device only; the value is per-user via {@link UserRoleHelper#getUserId()}.
 *
 * State persisted:
 *   - enabled (boolean): master switch for the daily reminder.
 *   - hour / minute (int): time-of-day to fire the reminder (24h).
 *   - streakReminder (boolean): include "keep your streak" copy in the notification.
 */
public final class NotificationPreferences {

    public static final String PREF_NAME = "notification_settings";

    private static final String KEY_ENABLED         = "reminder_enabled";
    private static final String KEY_HOUR            = "reminder_hour";
    private static final String KEY_MINUTE          = "reminder_minute";
    private static final String KEY_STREAK_REMINDER = "streak_reminder_enabled";

    // Defaults: 19:00 (giờ học buổi tối hợp lý cho học sinh), reminder enabled, streak hint on.
    public static final int DEFAULT_HOUR   = 19;
    public static final int DEFAULT_MINUTE = 0;

    private NotificationPreferences() {}

    public static SharedPreferences prefs() {
        return VsatApp.getInstance().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isReminderEnabled() {
        return prefs().getBoolean(KEY_ENABLED, false);
    }

    public static void setReminderEnabled(boolean enabled) {
        prefs().edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static int getReminderHour() {
        return prefs().getInt(KEY_HOUR, DEFAULT_HOUR);
    }

    public static int getReminderMinute() {
        return prefs().getInt(KEY_MINUTE, DEFAULT_MINUTE);
    }

    public static void setReminderTime(int hour, int minute) {
        int safeHour = Math.max(0, Math.min(23, hour));
        int safeMinute = Math.max(0, Math.min(59, minute));
        prefs().edit()
                .putInt(KEY_HOUR, safeHour)
                .putInt(KEY_MINUTE, safeMinute)
                .apply();
    }

    public static boolean isStreakReminderEnabled() {
        return prefs().getBoolean(KEY_STREAK_REMINDER, true);
    }

    public static void setStreakReminderEnabled(boolean enabled) {
        prefs().edit().putBoolean(KEY_STREAK_REMINDER, enabled).apply();
    }

    public static String formatReminderTime() {
        int h = getReminderHour();
        int m = getReminderMinute();
        return String.format(java.util.Locale.US, "%02d:%02d", h, m);
    }
}
