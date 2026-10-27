package com.example.v_sat_compass.notification;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.v_sat_compass.data.repository.GamificationRepository;
import com.example.v_sat_compass.util.NotificationPreferences;

/**
 * Fires once per day at the user-chosen time. Reads the latest streak from
 * {@link GamificationRepository} (F3) to craft a personal reminder.
 *
 * Intentionally not retro-active: if WorkManager schedules us late (Doze etc.) we
 * still post the current state and let the next periodic run pick up tomorrow.
 */
public class DailyReminderWorker extends Worker {

    public DailyReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        // Master switch: if the user turned off reminders since the schedule was
        // set, do not post anything.
        if (!NotificationPreferences.isReminderEnabled()) {
            return Result.success();
        }

        // Permission revoked at runtime -> skip silently, the user already saw
        // an in-app warning when they tried to enable.
        if (!NotificationHelper.hasPostPermission(context)) {
            return Result.success();
        }

        final String[] title = {context.getString(com.example.v_sat_compass.R.string.notif_reminder_title_default)};
        final String[] body  = {context.getString(com.example.v_sat_compass.R.string.notif_reminder_body_default)};

        GamificationRepository.getInstance().getState(context, state -> {
            if (state == null) return;
            if (NotificationPreferences.isStreakReminderEnabled() && state.getCurrentStreak() > 0) {
                int streak = state.getCurrentStreak();
                title[0] = context.getString(
                        com.example.v_sat_compass.R.string.notif_reminder_title_streak,
                        streak);
                body[0] = context.getString(
                        com.example.v_sat_compass.R.string.notif_reminder_body_streak,
                        streak,
                        com.example.v_sat_compass.data.model.GamificationState.DAILY_CHALLENGE_TARGET_HINT);
            } else if (state.getChallengeProgress() > 0
                    && !state.isChallengeCompleted()) {
                int remaining = com.example.v_sat_compass.data.model.GamificationState.DAILY_CHALLENGE_TARGET_HINT
                        - state.getChallengeProgress();
                body[0] = context.getString(
                        com.example.v_sat_compass.R.string.notif_reminder_body_challenge,
                        Math.max(remaining, 1));
            }
            NotificationHelper.postDailyReminder(context, title[0], body[0]);
        });

        return Result.success();
    }
}
