package com.example.v_sat_compass.ui.history;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.local.LocalExamDataSource;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.data.model.ExamHistoryEntry;
import com.example.v_sat_compass.data.repository.ExamHistoryRepository;
import com.example.v_sat_compass.ui.exam.ExamDetailActivity;
import com.example.v_sat_compass.ui.exam.session.ExamReviewActivity;

import java.util.List;

public class ExamHistoryActivity extends AppCompatActivity {

    private static final String TAG = "ExamHistoryActivity";
    private static final String KEY_SUBJECT_FILTER = "history_subject_filter";

    private RecyclerView rvHistory;
    private TextView tvEmpty;
    private ProgressBar progressLoading;
    private TextView tvStatAttempts, tvStatAvgScore, tvStatAccuracy;
    private TextView chipAll, chipMath, chipEnglish, chipPhysics;
    private ExamHistoryAdapter adapter;

    private String currentSubjectFilter = null; // null = tất cả

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_history);

        rvHistory       = findViewById(R.id.rvHistory);
        tvEmpty         = findViewById(R.id.tvEmpty);
        progressLoading = findViewById(R.id.progressLoading);
        tvStatAttempts  = findViewById(R.id.tvStatAttempts);
        tvStatAvgScore  = findViewById(R.id.tvStatAvgScore);
        tvStatAccuracy  = findViewById(R.id.tvStatAccuracy);
        chipAll         = findViewById(R.id.chipAll);
        chipMath        = findViewById(R.id.chipMath);
        chipEnglish     = findViewById(R.id.chipEnglish);
        chipPhysics     = findViewById(R.id.chipPhysics);

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExamHistoryAdapter();
        rvHistory.setAdapter(adapter);

        adapter.setOnReviewClickListener(this::openReview);

        // Khôi phục filter chip sau rotate
        if (savedInstanceState != null) {
            currentSubjectFilter = savedInstanceState.getString(KEY_SUBJECT_FILTER, null);
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        setupChipFilters();
        // Sync chip UI với filter đã khôi phục
        syncChipUi(currentSubjectFilter);

        loadStats();
        showLoadingState(true);
        loadHistory(currentSubjectFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Làm mới khi quay lại từ màn review
        loadStats();
        loadHistory(currentSubjectFilter);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SUBJECT_FILTER, currentSubjectFilter);
    }

    private void setupChipFilters() {
        chipAll.setOnClickListener(v -> selectChip(null));
        chipMath.setOnClickListener(v -> selectChip(getString(R.string.history_filter_math)));
        chipEnglish.setOnClickListener(v -> selectChip(getString(R.string.history_filter_english)));
        chipPhysics.setOnClickListener(v -> selectChip(getString(R.string.history_filter_physics)));
    }

    private void selectChip(String subject) {
        currentSubjectFilter = subject;
        syncChipUi(subject);
        showLoadingState(true);
        loadHistory(subject);
    }

    private void syncChipUi(String subject) {
        updateChipStyle(chipAll, subject == null);
        updateChipStyle(chipMath, getString(R.string.history_filter_math).equals(subject));
        updateChipStyle(chipEnglish, getString(R.string.history_filter_english).equals(subject));
        updateChipStyle(chipPhysics, getString(R.string.history_filter_physics).equals(subject));
    }

    private void updateChipStyle(TextView chip, boolean selected) {
        if (selected) {
            chip.setBackgroundResource(R.drawable.bg_chip_selected);
            chip.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void showLoadingState(boolean loading) {
        if (progressLoading != null) {
            progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (loading) {
            rvHistory.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void loadStats() {
        ExamHistoryRepository.getInstance().getStats(this, stats -> {
            tvStatAttempts.setText(String.valueOf(stats.totalAttempts));
            if (stats.totalAttempts > 0) {
                tvStatAvgScore.setText(String.valueOf(stats.avgScore));
                ExamHistoryRepository.getInstance().getAll(this, all -> {
                    if (all.isEmpty()) {
                        tvStatAccuracy.setText(getString(R.string.history_stat_empty));
                        return;
                    }
                    int tc = 0, tq = 0;
                    for (ExamHistoryEntry e : all) {
                        tc += e.getCorrectCount();
                        tq += e.getTotalQuestions();
                    }
                    int pct = tq > 0 ? (int) (tc * 100.0 / tq) : 0;
                    tvStatAccuracy.setText(pct + "%");
                });
            } else {
                tvStatAvgScore.setText(getString(R.string.history_stat_empty));
                tvStatAccuracy.setText(getString(R.string.history_stat_empty));
            }
        });
    }

    private void loadHistory(String subjectFilter) {
        ExamHistoryRepository.getInstance().getBySubject(this, subjectFilter, entries -> {
            showLoadingState(false);
            if (entries.isEmpty()) {
                rvHistory.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                // Phân biệt empty state: chưa có bài nào vs filter ra 0 kết quả
                if (subjectFilter == null) {
                    tvEmpty.setText(getString(R.string.history_empty_no_exams));
                } else {
                    tvEmpty.setText(getString(R.string.history_empty_no_filter_results));
                }
            } else {
                tvEmpty.setVisibility(View.GONE);
                rvHistory.setVisibility(View.VISIBLE);
            }
            adapter.setEntries(entries);
        });
    }

    private void openReview(ExamHistoryEntry entry) {
        // Kiểm tra pack đề còn tồn tại không
        Exam exam = LocalExamDataSource.getInstance().getExamDetail(this, entry.getExamId());
        if (exam == null) {
            Log.w(TAG, "openReview() exam not found for examId=" + entry.getExamId());
            // Vẫn cho mở review — ExamReviewActivity tự xử lý gracefully
        }
        try {
            Intent intent = new Intent(this, ExamReviewActivity.class);
            intent.putExtra(ExamReviewActivity.EXTRA_EXAM_ID, entry.getExamId());
            intent.putExtra(ExamReviewActivity.EXTRA_SELECTED_ANSWERS_JSON, entry.getSelectedAnswersJson());
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "openReview() failed to start ExamReviewActivity", e);
        }
    }

    /** Entry point từ ExamFragment/HomeFragment để mở màn History trực tiếp với filter. */
    public static Intent newIntent(android.content.Context context) {
        return new Intent(context, ExamHistoryActivity.class);
    }
}
