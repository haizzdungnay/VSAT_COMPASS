package com.example.v_sat_compass.ui.exam;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.ExamApi;
import com.example.v_sat_compass.data.local.LocalExamDataSource;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.databinding.ActivityExamDetailBinding;
import com.example.v_sat_compass.ui.exam.session.ExamSessionActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
        ExamApi api = ApiClient.getClient().create(ExamApi.class);
        api.getExamDetail(examId).enqueue(new Callback<ApiResponse<Exam>>() {
            @Override
            public void onResponse(Call<ApiResponse<Exam>> call, Response<ApiResponse<Exam>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    applyExam(response.body().getData());
                } else {
                    loadLocalFallback();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Exam>> call, Throwable t) {
                loadLocalFallback();
            }
        });
    }

    private void loadLocalFallback() {
        Toast.makeText(this, "Dang offline -- dung de mau", Toast.LENGTH_SHORT).show();
        Exam exam = LocalExamDataSource.getInstance().getExamDetail(this, examId);
        if (exam != null) {
            applyExam(exam);
        } else {
            Toast.makeText(this, "Khong tim thay de mau offline", Toast.LENGTH_SHORT).show();
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
        binding.tvDescription.setText(description != null && !description.isEmpty() ? description :
                "Đề thi mô phỏng bám sát cấu trúc kỳ thi V-SAT chính thức năm 2024. Nội dung bao gồm các phần kiến thức trọng tâm về Đại số, Giải tích, Hình học và Xác suất thống kê, được biên soạn bởi đội ngũ giáo viên giàu kinh nghiệm.");
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
