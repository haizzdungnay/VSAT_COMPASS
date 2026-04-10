package com.example.v_sat_compass.ui.exam.session;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.ExamApi;
import com.example.v_sat_compass.data.local.LocalExamDataSource;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.data.model.ExamSession;
import com.example.v_sat_compass.data.model.Question;
import com.example.v_sat_compass.databinding.ActivityExamSessionBinding;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private Set<Long> bookmarkedQuestions = new HashSet<>();
    // Cache questions fetched from API so we can score locally without extra requests
    private Map<Long, Question> questionCache = new HashMap<>();

    private CountDownTimer timer;
    private long sessionStartMillis;

    // Colors for option cards
    private int colorDefault;
    private int colorSelected;
    private int strokeDefault;
    private int strokeSelected;
    private final boolean clientSideProcessing = ApiClient.isClientSideExamProcessingEnabled();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExamSessionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        examApi = ApiClient.getClient().create(ExamApi.class);

        colorDefault = Color.WHITE;
        colorSelected = ContextCompat.getColor(this, R.color.answer_selected);
        strokeDefault = ContextCompat.getColor(this, R.color.text_hint);
        strokeSelected = ContextCompat.getColor(this, R.color.primary);

        examId = getIntent().getLongExtra("exam_id", 0);
        String title = getIntent().getStringExtra("exam_title");
        durationMinutes = getIntent().getIntExtra("duration_minutes", 60);
        totalQuestions = getIntent().getIntExtra("total_questions", 0);

        // Show subject name in top bar (use title as subject prefix)
        String displayTitle = title != null ? title : "Đề thi";
        binding.tvExamTitle.setText(displayTitle);

        binding.btnBack.setOnClickListener(v -> confirmExit());
        binding.btnPrevious.setOnClickListener(v -> navigateQuestion(-1));
        binding.btnNext.setOnClickListener(v -> navigateQuestion(1));
        binding.btnGrid.setOnClickListener(v -> showQuestionGrid());
        binding.btnBookmark.setOnClickListener(v -> toggleBookmark());

        startSession();
    }

    private void startSession() {
        sessionStartMillis = System.currentTimeMillis();

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
                    Toast.makeText(ExamSessionActivity.this, "Không thể bắt đầu bài thi", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ExamSession>> call, Throwable t) {
                sessionId = System.currentTimeMillis();
                sessionStartMillis = System.currentTimeMillis();
                loadExamDetailFromLocal();
                startTimer();
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
                loadExamDetailFromLocal();
            }
        });
    }

    private void loadExamDetailFromLocal() {
        Exam exam = LocalExamDataSource.getInstance().getExamDetail(this, examId);
        if (exam == null || exam.getQuestions() == null || exam.getQuestions().isEmpty()) {
            Toast.makeText(this, "Không có dữ liệu đề thi cục bộ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        questionIds.clear();
        for (Exam.ExamQuestion q : exam.getQuestions()) {
            questionIds.add(q.getQuestionId());
        }
        totalQuestions = questionIds.size();
        loadQuestion(0);
    }

    private void loadQuestion(int index) {
        if (index < 0 || index >= questionIds.size()) return;

        currentIndex = index;
        Long questionId = questionIds.get(index);

        binding.tvQuestionNumber.setText("Câu " + (index + 1) + "/" + totalQuestions);
        int progressPercent = (int) (((index + 1) * 100.0) / totalQuestions);
        binding.progressQuestion.setProgress(progressPercent);

        binding.btnPrevious.setEnabled(index > 0);
        binding.btnNext.setText(index == totalQuestions - 1 ? "Nộp bài" : "Tiếp theo");

        // Serve from cache first to avoid redundant network calls
        if (questionCache.containsKey(questionId)) {
            currentQuestion = questionCache.get(questionId);
            displayQuestion(currentQuestion);
            updateBookmarkIcon();
            return;
        }

        examApi.getSessionQuestion(sessionId, questionId).enqueue(new Callback<ApiResponse<Question>>() {
            @Override
            public void onResponse(Call<ApiResponse<Question>> call, Response<ApiResponse<Question>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    currentQuestion = response.body().getData();
                    questionCache.put(questionId, currentQuestion); // cache for scoring
                    displayQuestion(currentQuestion);
                    updateBookmarkIcon();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Question>> call, Throwable t) {
                Question localQuestion = LocalExamDataSource.getInstance().getQuestion(ExamSessionActivity.this, questionId);
                if (localQuestion != null) {
                    currentQuestion = localQuestion;
                    questionCache.put(questionId, localQuestion);
                    displayQuestion(currentQuestion);
                    updateBookmarkIcon();
                } else {
                    Toast.makeText(ExamSessionActivity.this, "Lỗi tải câu hỏi", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void displayQuestion(Question question) {
        binding.tvQuestionText.setText("Câu " + (currentIndex + 1) + ": " + question.getQuestionText());
        binding.llOptions.removeAllViews();

        if (question.getOptions() == null || question.getOptions().isEmpty()) return;

        List<Question.Option> opts = question.getOptions();
        Long alreadySelected = selectedAnswers.get(question.getId());

        // Build 2-column grid: pair options into rows
        for (int i = 0; i < opts.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = dpToPx(10);
            row.setLayoutParams(rowLp);

            // Left option card
            MaterialCardView card1 = buildOptionCard(opts.get(i), question.getId(), alreadySelected);
            LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp1.rightMargin = dpToPx(5);
            card1.setLayoutParams(lp1);
            row.addView(card1);

            // Right option card (if exists)
            if (i + 1 < opts.size()) {
                MaterialCardView card2 = buildOptionCard(opts.get(i + 1), question.getId(), alreadySelected);
                LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                lp2.leftMargin = dpToPx(5);
                card2.setLayoutParams(lp2);
                row.addView(card2);
            } else {
                // Placeholder to keep grid balanced
                View placeholder = new View(this);
                LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                lp2.leftMargin = dpToPx(5);
                placeholder.setLayoutParams(lp2);
                row.addView(placeholder);
            }

            binding.llOptions.addView(row);
        }
    }

    private MaterialCardView buildOptionCard(Question.Option option, Long questionId, Long alreadySelected) {
        boolean isSelected = option.getId() != null && option.getId().equals(alreadySelected);

        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dpToPx(10));
        card.setCardElevation(isSelected ? dpToPx(3) : dpToPx(1));
        card.setStrokeWidth(dpToPx(isSelected ? 2 : 1));
        card.setStrokeColor(isSelected ? strokeSelected : 0xFFDDDDDD);
        card.setCardBackgroundColor(isSelected ? colorSelected : colorDefault);
        card.setTag(option.getId());

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setGravity(Gravity.CENTER_VERTICAL);
        inner.setPadding(dpToPx(12), dpToPx(14), dpToPx(12), dpToPx(14));

        // Label: A, B, C, D
        TextView tvLabel = new TextView(this);
        String label = option.getOptionLabel() != null ? option.getOptionLabel() : "";
        tvLabel.setText(label + ".");
        tvLabel.setTextSize(15);
        tvLabel.setTextColor(isSelected
                ? ContextCompat.getColor(this, R.color.primary)
                : ContextCompat.getColor(this, R.color.text_secondary));
        tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        labelLp.rightMargin = dpToPx(8);
        tvLabel.setLayoutParams(labelLp);

        // Option text
        TextView tvText = new TextView(this);
        tvText.setText(option.getOptionText());
        tvText.setTextSize(14);
        tvText.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvText.setLayoutParams(textLp);

        inner.addView(tvLabel);
        inner.addView(tvText);

        // Checkmark if selected
        if (isSelected) {
            TextView tvCheck = new TextView(this);
            tvCheck.setText("✓");
            tvCheck.setTextSize(16);
            tvCheck.setTextColor(ContextCompat.getColor(this, R.color.primary));
            tvCheck.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams checkLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            checkLp.leftMargin = dpToPx(6);
            tvCheck.setLayoutParams(checkLp);
            inner.addView(tvCheck);
        }

        card.addView(inner);

        card.setOnClickListener(v -> {
            selectedAnswers.put(questionId, option.getId());
            // Refresh display to show new selection
            displayQuestion(currentQuestion);
            submitAnswer(questionId, option.getId());
        });

        return card;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void submitAnswer(Long questionId, Long optionId) {
        if (clientSideProcessing) {
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("questionId", questionId);
        body.put("selectedOptionId", optionId);
        body.put("isBookmarked", bookmarkedQuestions.contains(questionId));
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
        int answered = selectedAnswers.size();
        int unanswered = totalQuestions - answered;
        String msg = "Bạn đã làm " + answered + "/" + totalQuestions + " câu.";
        if (unanswered > 0) msg += "\nCòn " + unanswered + " câu chưa trả lời.";
        msg += "\n\nBạn có chắc chắn muốn nộp bài?";

        new AlertDialog.Builder(this)
                .setTitle("Nộp bài")
                .setMessage(msg)
                .setPositiveButton("Nộp bài", (d, w) -> submitExam())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void submitExam() {
        if (timer != null) timer.cancel();

        if (clientSideProcessing) {
            submitExamLocally();
            return;
        }

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
                    Toast.makeText(ExamSessionActivity.this, "Lỗi nộp bài. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ExamSession>> call, Throwable t) {
                submitExamLocally();
            }
        });
    }

    private void submitExamLocally() {
        int correct = 0;
        for (Map.Entry<Long, Long> entry : selectedAnswers.entrySet()) {
            Long qId = entry.getKey();
            Long selectedOptionId = entry.getValue();
            Question q = questionCache.get(qId);
            if (q != null && q.getOptions() != null) {
                for (Question.Option opt : q.getOptions()) {
                    if (opt.getId() != null && opt.getId().equals(selectedOptionId) && opt.isCorrect()) {
                        correct++;
                        break;
                    }
                }
            } else {
                // fallback to local data source if question not cached
                Long expectedOption = LocalExamDataSource.getInstance().getCorrectOptionId(this, qId);
                if (expectedOption != null && expectedOption.equals(selectedOptionId)) correct++;
            }
        }

        int total = totalQuestions <= 0 ? Math.max(questionIds.size(), 1) : totalQuestions;
        double score = (correct * 100.0) / total;
        int timeSpentSeconds = 0;
        if (sessionStartMillis > 0) {
            timeSpentSeconds = (int) ((System.currentTimeMillis() - sessionStartMillis) / 1000);
            if (timeSpentSeconds < 0) timeSpentSeconds = 0;
        }

        // POST final result to backend (fire-and-forget; result is already shown to user)
        if (sessionId != null) {
            Map<String, Object> resultBody = new HashMap<>();
            resultBody.put("correctAnswers", correct);
            resultBody.put("totalQuestions", total);
            resultBody.put("scorePercentage", score);
            resultBody.put("timeSpentSeconds", timeSpentSeconds);
            examApi.submitClientResult(sessionId, resultBody).enqueue(new Callback<ApiResponse<ExamSession>>() {
                @Override public void onResponse(Call<ApiResponse<ExamSession>> call, Response<ApiResponse<ExamSession>> response) {}
                @Override public void onFailure(Call<ApiResponse<ExamSession>> call, Throwable t) {}
            });
        }

        Intent intent = new Intent(ExamSessionActivity.this, ExamResultActivity.class);
        intent.putExtra("session_id", sessionId != null ? sessionId : System.currentTimeMillis());
        intent.putExtra("score", score);
        intent.putExtra("correct", correct);
        intent.putExtra("total", total);
        intent.putExtra("time_spent", timeSpentSeconds);
        startActivity(intent);
        finish();
    }

    private void startTimer() {
        long millis = durationMinutes * 60 * 1000L;
        timer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long ms) {
                int minutes = (int) (ms / 60000);
                int seconds = (int) ((ms % 60000) / 1000);
                binding.tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

                // Turn timer red when < 5 minutes
                if (ms < 300000) {
                    binding.tvTimer.setBackgroundResource(R.drawable.bg_timer_red);
                }
            }

            @Override
            public void onFinish() {
                binding.tvTimer.setText("00:00");
                Toast.makeText(ExamSessionActivity.this, "Hết giờ! Bài thi đang được nộp.", Toast.LENGTH_SHORT).show();
                submitExam();
            }
        }.start();
    }

    private void toggleBookmark() {
        if (currentQuestion == null) return;
        Long qId = currentQuestion.getId();
        if (bookmarkedQuestions.contains(qId)) {
            bookmarkedQuestions.remove(qId);
            binding.btnBookmark.setColorFilter(
                    ContextCompat.getColor(this, R.color.text_secondary));
        } else {
            bookmarkedQuestions.add(qId);
            binding.btnBookmark.setColorFilter(
                    ContextCompat.getColor(this, R.color.warning));
        }
    }

    private void updateBookmarkIcon() {
        if (currentQuestion != null && bookmarkedQuestions.contains(currentQuestion.getId())) {
            binding.btnBookmark.setColorFilter(
                    ContextCompat.getColor(this, R.color.warning));
        } else {
            binding.btnBookmark.setColorFilter(
                    ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void showQuestionGrid() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_question_grid, null);

        int answered = 0;
        for (Long qId : questionIds) {
            if (selectedAnswers.containsKey(qId)) answered++;
        }

        android.widget.TextView tvAnswered = dialogView.findViewById(R.id.tvAnsweredCount);
        android.widget.TextView tvBookmarked = dialogView.findViewById(R.id.tvBookmarkedCount);
        tvAnswered.setText("Đã làm: " + answered + "/" + totalQuestions);
        tvBookmarked.setText("Đã đánh dấu: " + bookmarkedQuestions.size());

        RecyclerView rvGrid = dialogView.findViewById(R.id.rvQuestionGrid);
        rvGrid.setLayoutManager(new GridLayoutManager(this, 7));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        QuestionGridAdapter adapter = new QuestionGridAdapter(
                totalQuestions, questionIds, selectedAnswers, bookmarkedQuestions, currentIndex,
                position -> {
                    dialog.dismiss();
                    loadQuestion(position);
                }
        );
        rvGrid.setAdapter(adapter);

        dialogView.findViewById(R.id.btnSubmitFromGrid).setOnClickListener(v -> {
            dialog.dismiss();
            confirmSubmit();
        });

        dialog.show();
    }

    private void confirmExit() {
        new AlertDialog.Builder(this)
                .setTitle("Thoát bài thi")
                .setMessage("Bài thi chưa được nộp. Bạn muốn làm gì?")
                .setPositiveButton("Nộp và thoát", (d, w) -> submitExam())
                .setNegativeButton("Tiếp tục thi", null)
                .setNeutralButton("Thoát không nộp", (d, w) -> finish())
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
