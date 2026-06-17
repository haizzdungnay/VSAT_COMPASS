package com.example.v_sat_compass.ui.exam.session;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.ScoreConstants;
import com.example.v_sat_compass.data.model.TopicStatsResponse;
import com.example.v_sat_compass.data.repository.StudentStatsRepository;
import com.example.v_sat_compass.databinding.ActivityExamResultBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ExamResultActivity extends AppCompatActivity {

    private ActivityExamResultBinding binding;

    private long examId;
    private String selectedAnswersJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExamResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        double score     = getIntent().getDoubleExtra("score", 0);
        int correct      = getIntent().getIntExtra("correct", 0);
        int total        = getIntent().getIntExtra("total", 0);
        int timeSpent    = getIntent().getIntExtra("time_spent", 0);
        String examSubject = getIntent().getStringExtra("exam_subject");
        boolean saveFailed = getIntent().getBooleanExtra("history_save_failed", false);

        examId             = getIntent().getLongExtra("exam_id", 0);
        selectedAnswersJson = getIntent().getStringExtra("selected_answers_json");

        // Dùng ScoreConstants.PERCENT_TO_VSAT thay vì magic number 12
        int displayScore = (int) (score * ScoreConstants.PERCENT_TO_VSAT);
        binding.tvScore.setText(String.valueOf(displayScore));
        binding.tvScoreMax.setText("/" + ScoreConstants.VSAT_MAX_SCORE);
        binding.tvCorrectCount.setText(correct + "/" + total);

        binding.progressScore.setProgress((int) score);

        int minutes = timeSpent / 60;
        int seconds = timeSpent % 60;
        binding.tvTimeSpent.setText(
                String.format(Locale.getDefault(), "%d:%02d", minutes, seconds));

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnBackToList.setOnClickListener(v -> finish());
        binding.btnViewDetail.setOnClickListener(v -> openReview());

        renderSubjectResult(examSubject, correct, total, (int) Math.round(score));
        loadWeakTopics();

        // Thông báo khi lưu lịch sử thất bại (storage đầy, permission…)
        if (saveFailed) {
            Snackbar.make(binding.getRoot(),
                    getString(R.string.history_save_error),
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private void openReview() {
        Intent intent = new Intent(this, ExamReviewActivity.class);
        intent.putExtra(ExamReviewActivity.EXTRA_EXAM_ID, examId);
        intent.putExtra(ExamReviewActivity.EXTRA_SELECTED_ANSWERS_JSON, selectedAnswersJson);
        startActivity(intent);
    }

    private void renderSubjectResult(String subject, int correct, int total, int percentage) {
        binding.llSubjectResults.removeAllViews();
        String label = subject != null && !subject.trim().isEmpty() ? subject : "Chung";
        binding.llSubjectResults.addView(buildMetricRow(label,
                percentage + "% - " + correct + "/" + total + " cau", percentage, R.drawable.progress_purple));
    }

    private void loadWeakTopics() {
        new StudentStatsRepository().loadWeakTopicStats(new StudentStatsRepository.TopicStatsCallback() {
            @Override
            public void onSuccess(List<TopicStatsResponse> topics) {
                renderWeakTopics(topics);
            }

            @Override
            public void onError(String message) {
                renderWeakTopics(new ArrayList<>());
            }
        });
    }

    private void renderWeakTopics(List<TopicStatsResponse> topics) {
        if (binding == null) return;
        binding.llWeakTopics.removeAllViews();
        List<TopicStatsResponse> rows = new ArrayList<>();
        if (topics != null) {
            for (TopicStatsResponse topic : topics) {
                if (topic.getTotal() > 0) rows.add(topic);
            }
        }
        rows.sort(Comparator.comparingInt(TopicStatsResponse::getPercentage));
        if (rows.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Chua co du lieu chu de sau bai thi nay");
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            empty.setTextSize(14);
            binding.llWeakTopics.addView(empty);
            return;
        }
        int count = Math.min(3, rows.size());
        for (int i = 0; i < count; i++) {
            TopicStatsResponse topic = rows.get(i);
            String label = topic.getTopicName() != null ? topic.getTopicName() : "Chu de";
            binding.llWeakTopics.addView(buildMetricRow(label,
                    topic.getPercentage() + "% - " + topic.getCorrect() + "/" + topic.getTotal() + " cau",
                    topic.getPercentage(), R.drawable.progress_orange));
        }
    }

    private View buildMetricRow(String title, String value, int progress, int progressDrawable) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams containerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        containerLp.bottomMargin = dp(12);
        container.setLayoutParams(containerLp);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        tvTitle.setTextSize(14);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tvValue.setTextSize(13);

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(Math.max(0, Math.min(100, progress)));
        bar.setProgressDrawable(ContextCompat.getDrawable(this, progressDrawable));
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(6));
        barLp.topMargin = dp(6);
        bar.setLayoutParams(barLp);

        row.addView(tvTitle);
        row.addView(tvValue);
        container.addView(row);
        container.addView(bar);
        return container;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
