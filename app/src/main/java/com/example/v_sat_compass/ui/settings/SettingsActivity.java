package com.example.v_sat_compass.ui.settings;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.notification.NotificationHelper;
import com.example.v_sat_compass.notification.ReminderScheduler;
import com.example.v_sat_compass.util.NotificationPreferences;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * F8 — local notification settings screen.
 *
 * Wires the F8 WorkManager reminder:
 *   - master switch
 *   - time picker
 *   - opt-in for the streak-hint copy
 *   - POST_NOTIFICATIONS permission flow on Android 13+
 */
public class SettingsActivity extends AppCompatActivity {

    private SwitchMaterial switchEnabled;
    private SwitchMaterial switchStreak;
    private TextView tvTimeValue;
    private TextView tvStatusBanner;
    private LinearLayout sectionReminder;
    private MaterialButton btnSave;

    private ActivityResultLauncher<String> requestPostNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPostNotifications = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> refreshBannerAndControls());
        setContentView(buildView());
        bindFromPrefs();
        refreshBannerAndControls();
    }

    private LinearLayout buildView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ContextCompat.getColor(this, R.color.background));
        root.setPadding(dp(16), dp(16), dp(16), dp(16));

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header);

        TextView title = new TextView(this);
        title.setText(R.string.settings_title);
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(title);

        MaterialButton close = new MaterialButton(this);
        close.setText(R.string.detail_btn_cancel);
        close.setOnClickListener(v -> finish());
        header.addView(close);

        tvStatusBanner = new TextView(this);
        tvStatusBanner.setTextSize(13);
        tvStatusBanner.setPadding(0, dp(10), 0, dp(10));
        root.addView(tvStatusBanner);

        // Reminder section
        sectionReminder = new LinearLayout(this);
        sectionReminder.setOrientation(LinearLayout.VERTICAL);
        root.addView(sectionReminder);

        sectionReminder.addView(sectionTitle(getString(R.string.settings_section_reminder)));

        // Master switch
        sectionReminder.addView(buildSwitchRow(
                R.string.settings_reminder_enabled,
                (button, checked) -> {
                    if (checked && !NotificationHelper.hasPostPermission(this)) {
                        requestPostNotificationsIfNeeded();
                    }
                    refreshBannerAndControls();
                }
        ));
        switchEnabled = (SwitchMaterial) sectionReminder.getChildAt(sectionReminder.getChildCount() - 1)
                .findViewById(R.id.rowSwitch);

        // Time picker row
        sectionReminder.addView(buildTimeRow());
        tvTimeValue = (TextView) sectionReminder.getChildAt(sectionReminder.getChildCount() - 1)
                .findViewById(R.id.tvTimeValue);

        // Streak copy switch
        sectionReminder.addView(buildSwitchRow(
                R.string.settings_streak_reminder,
                null));
        switchStreak = (SwitchMaterial) sectionReminder.getChildAt(sectionReminder.getChildCount() - 1)
                .findViewById(R.id.rowSwitch);

        // Save button
        btnSave = new MaterialButton(this);
        btnSave.setText(R.string.settings_save);
        btnSave.setOnClickListener(v -> saveAndSchedule());
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        saveLp.topMargin = dp(16);
        btnSave.setLayoutParams(saveLp);
        sectionReminder.addView(btnSave);

        return root;
    }

    private TextView sectionTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        lp.bottomMargin = dp(8);
        tv.setLayoutParams(lp);
        return tv;
    }

    private MaterialCardView buildSwitchRow(int titleRes, CompoundButton.OnCheckedChangeListener listener) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
        card.setRadius(dp(10));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(8);
        card.setLayoutParams(cardLp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.addView(row);

        TextView title = new TextView(this);
        title.setText(titleRes);
        title.setTextSize(14);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(title);

        SwitchMaterial sw = new SwitchMaterial(this);
        sw.setId(R.id.rowSwitch);
        row.addView(sw);
        if (listener != null) {
            sw.setOnCheckedChangeListener(listener);
        }
        return card;
    }

    private MaterialCardView buildTimeRow() {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
        card.setRadius(dp(10));
        card.setClickable(true);
        card.setFocusable(true);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(8);
        card.setLayoutParams(cardLp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.addView(row);

        TextView label = new TextView(this);
        label.setText(R.string.settings_reminder_time_label);
        label.setTextSize(14);
        label.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        label.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(label);

        TextView value = new TextView(this);
        value.setId(R.id.tvTimeValue);
        value.setTextSize(15);
        value.setTypeface(null, android.graphics.Typeface.BOLD);
        value.setTextColor(ContextCompat.getColor(this, R.color.primary));
        value.setHint(R.string.settings_reminder_time_hint);
        row.addView(value);

        card.setOnClickListener(v -> showTimePicker());
        value.setOnClickListener(v -> showTimePicker());
        return card;
    }

    private void showTimePicker() {
        int h = NotificationPreferences.getReminderHour();
        int m = NotificationPreferences.getReminderMinute();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            NotificationPreferences.setReminderTime(hourOfDay, minute);
            renderTime();
        }, h, m, true).show();
    }

    private void bindFromPrefs() {
        switchEnabled.setChecked(NotificationPreferences.isReminderEnabled());
        switchStreak.setChecked(NotificationPreferences.isStreakReminderEnabled());
        renderTime();
    }

    private void renderTime() {
        if (tvTimeValue != null) {
            tvTimeValue.setText(NotificationPreferences.formatReminderTime());
        }
    }

    private void refreshBannerAndControls() {
        boolean permGranted = NotificationHelper.hasPostPermission(this);
        boolean enabled = switchEnabled != null && switchEnabled.isChecked();

        if (enabled && !permGranted) {
            tvStatusBanner.setText(R.string.settings_permission_rationale);
            tvStatusBanner.setTextColor(ContextCompat.getColor(this, R.color.error));
            tvStatusBanner.setVisibility(View.VISIBLE);
        } else if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            tvStatusBanner.setText(R.string.settings_permission_denied);
            tvStatusBanner.setTextColor(ContextCompat.getColor(this, R.color.error));
            tvStatusBanner.setVisibility(View.VISIBLE);
        } else {
            tvStatusBanner.setVisibility(View.GONE);
        }
    }

    private void requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) return;
        try {
            requestPostNotifications.launch(Manifest.permission.POST_NOTIFICATIONS);
        } catch (Exception ignored) {
            // User denied multiple times -> silently keep the setting off.
        }
    }

    private void saveAndSchedule() {
        boolean enabled = switchEnabled.isChecked();
        NotificationPreferences.setReminderEnabled(enabled);
        NotificationPreferences.setStreakReminderEnabled(switchStreak.isChecked());
        NotificationPreferences.setReminderTime(
                NotificationPreferences.getReminderHour(),
                NotificationPreferences.getReminderMinute());

        if (enabled && !NotificationHelper.hasPostPermission(this)) {
            requestPostNotificationsIfNeeded();
        }

        ReminderScheduler.apply(getApplicationContext());
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
        refreshBannerAndControls();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
