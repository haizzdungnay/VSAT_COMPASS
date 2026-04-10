package com.example.v_sat_compass.ui.exam;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.databinding.ActivityExamDetailBinding;
import com.example.v_sat_compass.ui.exam.session.ExamSessionActivity;

public class ExamDetailActivity extends AppCompatActivity {

    private ActivityExamDetailBinding binding;
    private boolean isSaved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExamDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        long examId = getIntent().getLongExtra("exam_id", 0);
        String title = getIntent().getStringExtra("exam_title");
        String description = getIntent().getStringExtra("exam_description");
        String subject = getIntent().getStringExtra("exam_subject");
        int totalQuestions = getIntent().getIntExtra("total_questions", 0);
        int durationMinutes = getIntent().getIntExtra("duration_minutes", 60);

        binding.tvExamTitle.setText(title != null ? title : "Đề thi");
        binding.tvExamSubject.setText(subject != null ? subject : "");
        binding.tvDuration.setText("Thời gian: " + durationMinutes + " phút");
        binding.tvQuestionCount.setText("Số câu: " + totalQuestions);
        binding.tvDescription.setText(description != null && !description.isEmpty() ? description :
                "Đề thi mô phỏng bám sát cấu trúc kỳ thi V-SAT chính thức năm 2024. Nội dung bao gồm các phần kiến thức trọng tâm về Đại số, Giải tích, Hình học và Xác suất thống kê, được biên soạn bởi đội ngũ giáo viên giàu kinh nghiệm.");

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnShare.setOnClickListener(v ->
                Toast.makeText(this, "Chia sẻ đề thi: " + title, Toast.LENGTH_SHORT).show());

        binding.btnBookmark.setOnClickListener(v -> toggleSave());

        binding.btnStartExam.setOnClickListener(v -> {
            Intent intent = new Intent(this, ExamSessionActivity.class);
            intent.putExtra("exam_id", examId);
            intent.putExtra("exam_title", subject != null && !subject.isEmpty() ? subject : title);
            intent.putExtra("duration_minutes", durationMinutes);
            intent.putExtra("total_questions", totalQuestions);
            startActivity(intent);
            finish();
        });
    }

    private void toggleSave() {
        isSaved = !isSaved;
        if (isSaved) {
            binding.btnBookmark.setColorFilter(
                    ContextCompat.getColor(this, R.color.warning));
            Toast.makeText(this, "Đã lưu đề thi", Toast.LENGTH_SHORT).show();
        } else {
            binding.btnBookmark.setColorFilter(
                    ContextCompat.getColor(this, android.R.color.white));
            binding.btnBookmark.setAlpha(0.6f);
            Toast.makeText(this, "Đã bỏ lưu đề thi", Toast.LENGTH_SHORT).show();
        }
    }
}
