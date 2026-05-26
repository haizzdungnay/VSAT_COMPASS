package com.example.v_sat_compass.ui.exam;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.data.repository.ExamRepository;
import com.example.v_sat_compass.util.NetworkUtils;
import com.example.v_sat_compass.databinding.ActivityExamDetailBinding;
import com.example.v_sat_compass.ui.exam.session.ExamSessionActivity;

public class ExamDetailActivity extends AppCompatActivity {

    private ActivityExamDetailBinding binding;
    private boolean isSaved = false;
    private long examId;
    private String title;
    private String description;
    private String subject;
    private int totalQuestions;
    private int durationMinutes;
    private final boolean backendExamContent = ApiClient.USE_BACKEND_EXAM_CONTENT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExamDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        examId = getIntent().getLongExtra("exam_id", 0);
        title = getIntent().getStringExtra("exam_title");
        description = getIntent().getStringExtra("exam_description");
        subject = getIntent().getStringExtra("exam_subject");
        totalQuestions = getIntent().getIntExtra("total_questions", 0);
        durationMinutes = getIntent().getIntExtra("duration_minutes", 60);

        renderExamDetail();
        if (backendExamContent) {
            loadBackendDetail();
        }

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnShare.setOnClickListener(v ->
                Toast.makeText(this, "Chia sẻ đề thi: " + title, Toast.LENGTH_SHORT).show());

        binding.btnBookmark.setOnClickListener(v -> toggleSave());

        binding.btnStartExam.setOnClickListener(v -> {
            Intent intent = new Intent(this, ExamSessionActivity.class);
            intent.putExtra("exam_id", examId);
            intent.putExtra("exam_title", subject != null && !subject.isEmpty() ? subject : title);
            intent.putExtra("exam_subject", subject != null ? subject : "");
            intent.putExtra("duration_minutes", durationMinutes);
            intent.putExtra("total_questions", totalQuestions);
            startActivity(intent);
            finish();
        });
    }

    private void loadBackendDetail() {
        if (!NetworkUtils.isOnline(this)) {
            loadLocalFallback(null);
            return;
        }
        ExamRepository.getInstance().loadExamDetail(examId, new ExamRepository.ExamCallback() {
            @Override
            public void onSuccess(Exam exam) {
                applyExam(exam);
            }

            @Override
            public void onError(String message) {
                loadLocalFallback(message);
            }
        });
    }

    private void loadLocalFallback(String message) {
        Toast.makeText(this,
                message != null ? message : getString(R.string.exam_offline_fallback),
                Toast.LENGTH_SHORT).show();
        Exam exam = ExamRepository.getInstance().getLocalExamDetail(this, examId);
        if (exam != null) {
            applyExam(exam);
        } else {
            Toast.makeText(this, getString(R.string.exam_not_found_offline), Toast.LENGTH_SHORT).show();
        }
    }

    private void applyExam(Exam exam) {
        if (exam.getTitle() != null) title = exam.getTitle();
        if (exam.getDescription() != null) description = exam.getDescription();
        if (exam.getSubjectName() != null) subject = exam.getSubjectName();
        if (exam.getTotalQuestions() > 0) totalQuestions = exam.getTotalQuestions();
        if (exam.getDurationMinutes() > 0) durationMinutes = exam.getDurationMinutes();
        renderExamDetail();
    }

    private void renderExamDetail() {
        binding.tvExamTitle.setText(title != null ? title : "Đề thi");
        binding.tvExamSubject.setText(subject != null ? subject : "");
        binding.tvDuration.setText("Thời gian: " + durationMinutes + " phút");
        binding.tvQuestionCount.setText("Số câu: " + totalQuestions);
        binding.tvDescription.setText(description != null && !description.isEmpty()
                ? description
                : getString(R.string.exam_description_empty));
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
