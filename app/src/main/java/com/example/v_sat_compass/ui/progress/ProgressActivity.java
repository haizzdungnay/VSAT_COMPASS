package com.example.v_sat_compass.ui.progress;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.ExamHistoryEntry;
import com.example.v_sat_compass.data.repository.ExamHistoryRepository;
import com.example.v_sat_compass.databinding.ActivityProgressBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProgressActivity extends AppCompatActivity {
    private ActivityProgressBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProgressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.tvBack.setOnClickListener(v -> finish());
        loadProgress();
    }

    private void loadProgress() {
        ExamHistoryRepository.getInstance().getAll(this, entries -> {
            List<ExamHistoryEntry> sorted = new ArrayList<>(entries);
            Collections.sort(sorted, (a, b) -> Long.compare(a.getSubmittedAtMillis(), b.getSubmittedAtMillis()));
            render(sorted);
        });
    }

    private void render(List<ExamHistoryEntry> entries) {
        boolean enoughData = entries.size() >= 2;
        binding.contentProgress.setVisibility(enoughData ? View.VISIBLE : View.GONE);
        binding.contentEmpty.setVisibility(enoughData ? View.GONE : View.VISIBLE);
        if (!enoughData) return;

        List<Integer> scores = new ArrayList<>();
        long totalSeconds = 0L;
        int totalQuestions = 0;
        Map<String, SubjectAggregate> bySubject = new LinkedHashMap<>();

        for (ExamHistoryEntry entry : entries) {
            scores.add(entry.getScore());
            totalSeconds += Math.max(0L, entry.getTimeSpentSeconds());
            totalQuestions += Math.max(0, entry.getTotalQuestions());
            String subject = normalizeSubject(entry.getSubject());
            SubjectAggregate aggregate = bySubject.get(subject);
            if (aggregate == null) {
                aggregate = new SubjectAggregate();
                bySubject.put(subject, aggregate);
            }
            aggregate.add(entry);
        }

        binding.chartTrend.setScores(scores);
        binding.tvAttempts.setText(String.valueOf(entries.size()));
        binding.tvAvgTime.setText(totalQuestions > 0 ? String.valueOf(Math.round(totalSeconds * 1.0 / totalQuestions)) : "--");
        renderSubjects(bySubject);
    }

    private void renderSubjects(Map<String, SubjectAggregate> bySubject) {
        binding.subjectBreakdown.removeAllViews();
        for (Map.Entry<String, SubjectAggregate> entry : bySubject.entrySet()) {
            SubjectAggregate aggregate = entry.getValue();
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dp(10), 0, dp(6));
            binding.subjectBreakdown.addView(row);

            TextView title = new TextView(this);
            title.setText(entry.getKey() + "  " + aggregate.averageScore() + "/1200  "
                    + aggregate.averageAccuracy() + "%");
            title.setTextColor(getResources().getColor(R.color.text_primary, null));
            title.setTextSize(14);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(title);

            ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(1200);
            bar.setProgress(aggregate.averageScore());
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(8));
            barLp.topMargin = dp(6);
            bar.setLayoutParams(barLp);
            row.addView(bar);

            TextView detail = new TextView(this);
            detail.setText(aggregate.count + " attempts, " + aggregate.averageSecondsPerQuestion() + " sec/question");
            detail.setTextColor(getResources().getColor(R.color.text_secondary, null));
            detail.setTextSize(12);
            row.addView(detail);
        }
    }

    private String normalizeSubject(String subject) {
        if (subject == null || subject.trim().isEmpty()) return "General";
        return subject.trim();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class SubjectAggregate {
        int count;
        int scoreSum;
        int correctSum;
        int questionSum;
        long secondsSum;

        void add(ExamHistoryEntry entry) {
            count += 1;
            scoreSum += entry.getScore();
            correctSum += Math.max(0, entry.getCorrectCount());
            questionSum += Math.max(0, entry.getTotalQuestions());
            secondsSum += Math.max(0L, entry.getTimeSpentSeconds());
        }

        int averageScore() {
            return count > 0 ? Math.round(scoreSum * 1f / count) : 0;
        }

        int averageAccuracy() {
            return questionSum > 0 ? Math.round(correctSum * 100f / questionSum) : 0;
        }

        int averageSecondsPerQuestion() {
            return questionSum > 0 ? Math.round(secondsSum * 1f / questionSum) : 0;
        }
    }
}
