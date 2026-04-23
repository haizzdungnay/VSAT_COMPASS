package com.example.v_sat_compass.ui.exam.session;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.ScoreConstants;
import com.example.v_sat_compass.databinding.ActivityExamResultBinding;
import com.google.android.material.snackbar.Snackbar;

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
}
