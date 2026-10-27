package com.example.v_sat_compass.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.v_sat_compass.MainActivity;
import com.example.v_sat_compass.R;

/**
 * Single source of truth for the F8 reminder notification channel + posting.
 * Channel id is stable so we can update its name/description later without
 * breaking post-O behaviour.
 */
public final class NotificationHelper {

    public static final String CHANNEL_ID_REMINDER = "vsat_daily_reminder";

    private static final int NOTIFICATION_ID_REMINDER = 4001;

    private NotificationHelper() {}

    /**
     * Idempotent: safe to call repeatedly (e.g. on every Application.onCreate).
     */
    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = ContextCompat.getSystemService(context, NotificationManager.class);
        if (manager == null) return;
        NotificationChannel existing = manager.getNotificationChannel(CHANNEL_ID_REMINDER);
        if (existing != null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID_REMINDER,
                context.getString(R.string.notif_channel_reminder_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription(context.getString(R.string.notif_channel_reminder_desc));
        manager.createNotificationChannel(channel);
    }

    /**
     * On Android 13+ the OS requires runtime permission for posting notifications.
     * Returns true if we currently hold POST_NOTIFICATIONS (or the platform doesn't need it).
     */
    public static boolean hasPostPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static int postDailyReminder(Context context, String title, String body) {
        ensureChannel(context);
        if (!hasPostPermission(context)) return -1;

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_REMINDER)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        try {
            managerCompat.notify(NOTIFICATION_ID_REMINDER, builder.build());
        } catch (SecurityException ignored) {
            // Defensive: per docs notify() shouldn't throw once permission is held,
            // but some OEMs have historically done so.
            return -1;
        }
        return NOTIFICATION_ID_REMINDER;
    }

    public static int postDailyReminderRequest(Context context) {
        return ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED ? 0 : 1;
    }
}
