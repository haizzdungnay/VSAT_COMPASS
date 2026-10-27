package com.example.v_sat_compass.ui.leaderboard;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.LeaderboardEntry;
import com.example.v_sat_compass.data.model.LeaderboardMe;
import com.example.v_sat_compass.data.repository.LeaderboardRepository;
import com.example.v_sat_compass.util.UserRoleHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;
import java.util.Locale;

/**
 * F9 — leaderboard screen.
 * Lists top opt-in users; toggle controls the current user's opt-in.
 * Users who have not opted in are NOT shown in the list (server filters them).
 */
public class LeaderboardActivity extends AppCompatActivity {

    private static final int DEFAULT_LIMIT = 20;

    private final LeaderboardRepository repository = new LeaderboardRepository();

    private LinearLayout listContainer;
    private TextView tvEmpty;
    private TextView tvBanner;
    private SwitchMaterial switchOptIn;

    /** True while we are programmatically toggling to reflect server state. */
    private boolean syncingFromServer = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildView());
        wireOptInListener();
        loadMyStatus();
        loadTop();
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
        title.setText(R.string.leaderboard_title);
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(title);

        MaterialButton close = new MaterialButton(this);
        close.setText(R.string.detail_btn_cancel);
        close.setOnClickListener(v -> finish());
        header.addView(close);

        // Opt-in card
        root.addView(buildOptInCard());

        // Empty placeholder
        tvEmpty = new TextView(this);
        tvEmpty.setText(R.string.leaderboard_empty_subtitle);
        tvEmpty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tvEmpty.setTextSize(14);
        tvEmpty.setVisibility(View.GONE);
        tvEmpty.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams emptyLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        emptyLp.topMargin = dp(20);
        tvEmpty.setLayoutParams(emptyLp);
        root.addView(tvEmpty);

        // List
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        listLp.topMargin = dp(12);
        listContainer.setLayoutParams(listLp);
        root.addView(listContainer);

        return root;
    }

    private View buildOptInCard() {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
        card.setRadius(dp(10));
        card.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(content);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(row);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(col);

        TextView label = new TextView(this);
        label.setText(R.string.leaderboard_opt_in_label);
        label.setTextSize(15);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        label.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        col.addView(label);

        TextView hint = new TextView(this);
        hint.setText(R.string.leaderboard_opt_in_hint);
        hint.setTextSize(12);
        hint.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        hint.setPadding(0, dp(2), 0, 0);
        col.addView(hint);

        switchOptIn = new SwitchMaterial(this);
        row.addView(switchOptIn);

        tvBanner = new TextView(this);
        tvBanner.setTextSize(12);
        tvBanner.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tvBanner.setPadding(0, dp(8), 0, 0);
        tvBanner.setVisibility(View.GONE);
        content.addView(tvBanner);

        return card;
    }

    private void wireOptInListener() {
        switchOptIn.setOnCheckedChangeListener((CompoundButton button, boolean checked) -> {
            if (syncingFromServer) return;
            button.setEnabled(false);
            repository.setOptIn(checked, new LeaderboardRepository.MeCallback() {
                @Override
                public void onSuccess(LeaderboardMe me) {
                    button.setEnabled(true);
                    applyMe(me, /* refreshList= */ true);
                    Toast.makeText(LeaderboardActivity.this,
                            R.string.leaderboard_opt_in_updated, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    button.setEnabled(true);
                    // Revert toggle
                    syncingFromServer = true;
                    switchOptIn.setChecked(!checked);
                    syncingFromServer = false;
                    Toast.makeText(LeaderboardActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void loadMyStatus() {
        repository.getMe(new LeaderboardRepository.MeCallback() {
            @Override
            public void onSuccess(LeaderboardMe me) {
                applyMe(me, /* refreshList= */ false);
            }

            @Override
            public void onError(String message) {
                // Banner stays hidden; the toggle is still usable.
            }
        });
    }

    private void applyMe(LeaderboardMe me, boolean refreshList) {
        if (me == null) return;
        syncingFromServer = true;
        switchOptIn.setChecked(me.isOptIn());
        syncingFromServer = false;

        if (!me.isOptIn()) {
            tvBanner.setVisibility(View.VISIBLE);
            tvBanner.setText(R.string.leaderboard_you_hidden);
        } else {
            tvBanner.setVisibility(View.GONE);
        }
        if (refreshList) loadTop();
    }

    private void loadTop() {
        repository.getTop(DEFAULT_LIMIT, new LeaderboardRepository.EntriesCallback() {
            @Override
            public void onSuccess(List<LeaderboardEntry> entries) {
                renderEntries(entries);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(LeaderboardActivity.this,
                        R.string.leaderboard_load_error, Toast.LENGTH_SHORT).show();
                renderEntries(java.util.Collections.emptyList());
            }
        });
    }

    private void renderEntries(List<LeaderboardEntry> entries) {
        listContainer.removeAllViews();
        boolean empty = entries == null || entries.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) return;
        long currentUserId = UserRoleHelper.getUserId();
        for (LeaderboardEntry entry : entries) {
            listContainer.addView(buildEntryCard(entry, currentUserId));
        }
    }

    private MaterialCardView buildEntryCard(LeaderboardEntry entry, long currentUserId) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
        card.setRadius(dp(10));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = dp(8);
        card.setLayoutParams(cardLp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.addView(row);

        TextView rank = new TextView(this);
        rank.setText(String.format(Locale.US, "#%d", entry.getRank()));
        rank.setTextSize(18);
        rank.setTypeface(null, android.graphics.Typeface.BOLD);
        rank.setTextColor(ContextCompat.getColor(this, R.color.primary));
        rank.setMinWidth(dp(40));
        row.addView(rank);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(12), 0, 0, 0);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(col);

        TextView name = new TextView(this);
        String displayName = entry.getDisplayName() != null && !entry.getDisplayName().isEmpty()
                ? entry.getDisplayName() : "User";
        if (entry.getUserId() != null && entry.getUserId() == currentUserId) {
            name.setText(displayName + "  •  " + getString(R.string.leaderboard_you));
        } else {
            name.setText(displayName);
        }
        name.setTextSize(15);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        name.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        col.addView(name);

        TextView meta = new TextView(this);
        meta.setText(String.format(Locale.US, "%d bai", entry.getExamCount()));
        meta.setTextSize(12);
        meta.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        col.addView(meta);

        TextView score = new TextView(this);
        score.setText(String.format(Locale.US, "%d", entry.getTotalScore()));
        score.setTextSize(20);
        score.setTypeface(null, android.graphics.Typeface.BOLD);
        score.setTextColor(ContextCompat.getColor(this, R.color.success));
        row.addView(score);

        return card;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
