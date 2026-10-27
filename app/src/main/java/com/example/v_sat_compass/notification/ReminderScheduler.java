package com.example.v_sat_compass.notification;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.v_sat_compass.util.NotificationPreferences;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Owns the lifecycle of the F8 daily reminder WorkManager schedule.
 *
 * Why WorkManager and not AlarmManager: we don't need exact-time delivery, and
 * WorkManager is the recommended modern path for deferrable work. We use the
 * "next run delay" trick to align the first run with the user-chosen time.
 */
public final class ReminderScheduler {

    public static final String UNIQUE_NAME = "vsat_daily_reminder";

    private ReminderScheduler() {}

    /**
     * Reconciles the schedule with the current preferences.
     *  - If disabled: cancel any pending work.
     *  - If enabled: enqueue a periodic work with the correct initial delay.
     */
    public static void apply(Context context) {
        WorkManager wm = WorkManager.getInstance(context.getApplicationContext());
        if (!NotificationPreferences.isReminderEnabled()) {
            wm.cancelUniqueWork(UNIQUE_NAME);
            return;
        }
        PeriodicWorkRequest request = buildRequest();
        wm.enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
        );
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(UNIQUE_NAME);
    }

    private static PeriodicWorkRequest buildRequest() {
        int hour = NotificationPreferences.getReminderHour();
        int minute = NotificationPreferences.getReminderMinute();
        long initialDelayMillis = computeInitialDelayMillis(hour, minute);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build();

        return new PeriodicWorkRequest.Builder(DailyReminderWorker.class, 1, TimeUnit.DAYS)
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .addTag(UNIQUE_NAME)
                .build();
    }

    /**
     * Milliseconds from "now" until the next occurrence of the given local hour:minute.
     * WorkManager uses wall-clock time via its own scheduler; this just makes the
     * first run land on the user-chosen time so the user doesn't have to wait
     * 24h on the first day.
     */
    static long computeInitialDelayMillis(int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar target = (Calendar) now.clone();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1);
        }
        return target.getTimeInMillis() - now.getTimeInMillis();
    }
}
