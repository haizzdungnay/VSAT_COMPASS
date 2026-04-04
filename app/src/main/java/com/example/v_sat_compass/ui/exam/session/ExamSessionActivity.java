package com.example.v_sat_compass.ui.exam.session;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.ExamApi;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.data.model.ExamSession;
import com.example.v_sat_compass.data.model.Question;
import com.example.v_sat_compass.databinding.ActivityExamSessionBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExamSessionActivity extends AppCompatActivity {

    private ActivityExamSessionBinding binding;
    private ExamApi examApi;

    private Long sessionId;
    private long examId;
    private int durationMinutes;
    private int totalQuestions;

    private List<Long> questionIds = new ArrayList<>();
    private int currentIndex = 0;
    private Question currentQuestion;
    private Map<Long, Long> selectedAnswers = new HashMap<>();

    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExamSessionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        examApi = ApiClient.getClient().create(ExamApi.class);

        examId = getIntent().getLongExtra("exam_id", 0);
        String title = getIntent().getStringExtra("exam_title");
        durationMinutes = getIntent().getIntExtra("duration_minutes", 60);
        totalQuestions = getIntent().getIntExtra("total_questions", 0);

        binding.tvExamTitle.setText(title);

        binding.btnBack.setOnClickListener(v -> confirmExit());
        binding.btnPrevious.setOnClickListener(v -> navigateQuestion(-1));
        binding.btnNext.setOnClickListener(v -> navigateQuestion(1));

        startSession();
    }

    private void startSession() {
        Map<String, Long> body = new HashMap<>();
        body.put("examId", examId);

        examApi.startSession(body).enqueue(new Callback<ApiResponse<ExamSession>>() {
            @Override
            public void onResponse(Call<ApiResponse<ExamSession>> call, Response<ApiResponse<ExamSession>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    ExamSession session = response.body().getData();
                    sessionId = session.getId();
                    loadExamDetail();
                    startTimer();
                } else {
                    Toast.makeText(ExamSessionActivity.this, "Khong the bat dau bai thi", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ExamSession>> call, Throwable t) {
                Toast.makeText(ExamSessionActivity.this, "Loi ket noi", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadExamDetail() {
        examApi.getExamDetail(examId).enqueue(new Callback<ApiResponse<Exam>>() {
            @Override
            public void onResponse(Call<ApiResponse<Exam>> call, Response<ApiResponse<Exam>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Exam exam = response.body().getData();
                    if (exam.getQuestions() != null) {
                        for (Exam.ExamQuestion q : exam.getQuestions()) {
                            questionIds.add(q.getQuestionId());
                        }
                        totalQuestions = questionIds.size();
                        loadQuestion(0);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Exam>> call, Throwable t) {
                Toast.makeText(ExamSessionActivity.this, "Loi tai de thi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadQuestion(int index) {
        if (index < 0 || index >= questionIds.size()) return;

        currentIndex = index;
        Long questionId = questionIds.get(index);

        binding.tvQuestionNumber.setText("Cau " + (index + 1) + "/" + totalQuestions);
        binding.progressQuestion.setMax(totalQuestions);
        binding.progressQuestion.setProgress(index + 1);

        binding.btnPrevious.setEnabled(index > 0);
        binding.btnNext.setText(index == totalQuestions - 1 ? "Nop bai" : "Tiep");

        examApi.getSessionQuestion(sessionId, questionId).enqueue(new Callback<ApiResponse<Question>>() {
            @Override
            public void onResponse(Call<ApiResponse<Question>> call, Response<ApiResponse<Question>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    currentQuestion = response.body().getData();
                    displayQuestion(currentQuestion);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Question>> call, Throwable t) {
                Toast.makeText(ExamSessionActivity.this, "Loi tai cau hoi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayQuestion(Question question) {
        binding.tvQuestionText.setText(question.getQuestionText());
        binding.rgOptions.removeAllViews();

        if (question.getOptions() != null) {
            for (Question.Option option : question.getOptions()) {
                RadioButton rb = new RadioButton(this);
                rb.setId(View.generateViewId());
                rb.setText(option.getOptionLabel() + ". " + option.getOptionText());
                rb.setTextSize(15);
                rb.setPadding(8, 16, 8, 16);
                rb.setTag(option.getId());
                binding.rgOptions.addView(rb);

                if (selectedAnswers.containsKey(question.getId()) &&
                        selectedAnswers.get(question.getId()).equals(option.getId())) {
                    rb.setChecked(true);
                }
            }
        }

        binding.rgOptions.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selected = group.findViewById(checkedId);
            if (selected != null && selected.getTag() != null) {
                Long optionId = (Long) selected.getTag();
                selectedAnswers.put(question.getId(), optionId);
                submitAnswer(question.getId(), optionId);
            }
        });
    }

    private void submitAnswer(Long questionId, Long optionId) {
        Map<String, Object> body = new HashMap<>();
        body.put("questionId", questionId);
        body.put("selectedOptionId", optionId);
        body.put("isBookmarked", false);
        body.put("timeSpentSeconds", 0);

        examApi.submitAnswer(sessionId, body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {}

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
        });
    }

    private void navigateQuestion(int direction) {
        int newIndex = currentIndex + direction;
        if (newIndex >= 0 && newIndex < totalQuestions) {
            loadQuestion(newIndex);
        } else if (newIndex >= totalQuestions) {
            confirmSubmit();
        }
    }

    private void confirmSubmit() {
        new AlertDialog.Builder(this)
                .setTitle("Nop bai")
                .setMessage("Ban co chac chan muon nop bai?")
                .setPositiveButton("Nop bai", (d, w) -> submitExam())
                .setNegativeButton("Huy", null)
                .show();
    }

    private void submitExam() {
        if (timer != null) timer.cancel();

        examApi.submitSession(sessionId).enqueue(new Callback<ApiResponse<ExamSession>>() {
            @Override
            public void onResponse(Call<ApiResponse<ExamSession>> call, Response<ApiResponse<ExamSession>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    ExamSession result = response.body().getData();
                    Intent intent = new Intent(ExamSessionActivity.this, ExamResultActivity.class);
                    intent.putExtra("session_id", result.getId());
                    intent.putExtra("score", result.getScorePercentage());
                    intent.putExtra("correct", result.getCorrectAnswers());
                    intent.putExtra("total", result.getTotalQuestions());
                    intent.putExtra("time_spent", result.getTimeSpentSeconds());
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(ExamSessionActivity.this, "Loi nop bai", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ExamSession>> call, Throwable t) {
                Toast.makeText(ExamSessionActivity.this, "Loi ket noi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startTimer() {
        long millis = durationMinutes * 60 * 1000L;
        timer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long ms) {
                int minutes = (int) (ms / 60000);
                int seconds = (int) ((ms % 60000) / 1000);
                binding.tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                binding.tvTimer.setText("00:00");
                Toast.makeText(ExamSessionActivity.this, "Het gio!", Toast.LENGTH_SHORT).show();
                submitExam();
            }
        }.start();
    }

    private void confirmExit() {
        new AlertDialog.Builder(this)
                .setTitle("Thoat bai thi")
                .setMessage("Bai thi chua duoc nop. Ban co chac chan muon thoat?")
                .setPositiveButton("Nop va thoat", (d, w) -> submitExam())
                .setNegativeButton("Tiep tuc", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        confirmExit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }
}
