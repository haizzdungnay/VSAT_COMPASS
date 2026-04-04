package com.example.v_sat_compass.ui.exam.session;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.v_sat_compass.databinding.ActivityExamResultBinding;

import java.util.Locale;

public class ExamResultActivity extends AppCompatActivity {

    private ActivityExamResultBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExamResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        double score = getIntent().getDoubleExtra("score", 0);
        int correct = getIntent().getIntExtra("correct", 0);
        int total = getIntent().getIntExtra("total", 0);
        int timeSpent = getIntent().getIntExtra("time_spent", 0);

        binding.tvScore.setText(String.format(Locale.getDefault(), "%.0f%%", score));
        binding.tvCorrectCount.setText(correct + "/" + total);

        int minutes = timeSpent / 60;
        int seconds = timeSpent % 60;
        binding.tvTimeSpent.setText(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds));

        binding.tvPassStatus.setText(score >= 50 ? "DAT" : "CHUA DAT");

        binding.btnBackToList.setOnClickListener(v -> finish());
    }
}
