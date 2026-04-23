package com.example.v_sat_compass.ui.exam.session;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import com.example.v_sat_compass.data.local.LocalExamDataSource;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.data.model.Question;
import com.example.v_sat_compass.databinding.ActivityExamReviewBinding;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExamReviewActivity extends AppCompatActivity {

    private static final String TAG = "ExamReviewActivity";

    public static final String EXTRA_EXAM_ID = "review_exam_id";
    public static final String EXTRA_SELECTED_ANSWERS_JSON = "review_selected_answers_json";

    private static final String KEY_CURRENT_INDEX = "review_current_index";

    private ActivityExamReviewBinding binding;

    private long examId;
    /** questionId → selectedOptionId; thiếu key = câu chưa làm */
    private Map<Long, Long> selectedAnswers = new HashMap<>();

    private List<Long> questionIds = new ArrayList<>();
    private int currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExamReviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        examId = getIntent().getLongExtra(EXTRA_EXAM_ID, 0);
        String answersJson = getIntent().getStringExtra(EXTRA_SELECTED_ANSWERS_JSON);

        // Phòng thủ: examId không hợp lệ
        if (examId == 0) {
            Log.e(TAG, "onCreate() called with invalid examId=0");
            Toast.makeText(this, getString(R.string.review_question_load_error), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Parse selected answers từ JSON — log nếu lỗi, tiếp tục với map rỗng
        if (answersJson != null && !answersJson.isEmpty()) {
            try {
                Type type = new TypeToken<HashMap<Long, Long>>() {}.getType();
                Map<Long, Long> parsed = new Gson().fromJson(answersJson, type);
                if (parsed != null) selectedAnswers = parsed;
            } catch (JsonSyntaxException e) {
                Log.w(TAG, "onCreate() failed to parse selectedAnswers JSON, continuing with empty", e);
            }
        }

        loadQuestionsFromLocal();

        // Phòng thủ: không có câu hỏi nào
        if (questionIds.isEmpty()) {
            Log.w(TAG, "onCreate() no questions loaded for examId=" + examId);
            Toast.makeText(this, getString(R.string.review_question_load_error), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Khôi phục vị trí khi rotate
        if (savedInstanceState != null) {
            currentIndex = savedInstanceState.getInt(KEY_CURRENT_INDEX, 0);
            if (currentIndex >= questionIds.size()) currentIndex = 0;
        }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnPrevious.setOnClickListener(v -> navigate(-1));
        binding.btnNext.setOnClickListener(v -> navigate(1));
        binding.btnAllQuestions.setOnClickListener(v -> showQuestionGridDialog());

        showQuestion(currentIndex);
    }

    private void loadQuestionsFromLocal() {
        Exam exam = LocalExamDataSource.getInstance().getExamDetail(this, examId);
        if (exam != null && exam.getQuestions() != null) {
            for (Exam.ExamQuestion eq : exam.getQuestions()) {
                questionIds.add(eq.getQuestionId());
            }
        }
    }

    private void showQuestion(int index) {
        if (index < 0 || index >= questionIds.size()) return;
        currentIndex = index;

        Long questionId = questionIds.get(index);
        Question question = LocalExamDataSource.getInstance().getQuestion(this, questionId);

        int total = questionIds.size();

        binding.tvQuestionCounter.setText(getString(R.string.review_counter_format, index + 1, total));
        binding.tvQuestionBadge.setText(getString(R.string.review_question_label, index + 1));
        int progress = (int) (((index + 1) * 100.0) / total);
        binding.progressReview.setProgress(progress);

        binding.scrollContent.smoothScrollTo(0, 0);

        binding.btnPrevious.setEnabled(index > 0);
        binding.btnNext.setEnabled(index < total - 1);

        if (question == null) {
            // Pack đề có thể đã thay đổi sau khi bài cũ được lưu
            binding.tvQuestionText.setText(getString(R.string.review_question_unavailable));
            binding.tvUnansweredBadge.setVisibility(View.GONE);
            binding.llOptions.removeAllViews();
            binding.tvExplanation.setText(getString(R.string.review_explanation_placeholder));
            return;
        }

        Long selectedOptionId = selectedAnswers.get(questionId);
        boolean isAnswered = selectedOptionId != null;

        // Kiểm tra selectedOptionId còn hợp lệ trong pack hiện tại
        if (isAnswered && !optionExistsInQuestion(question, selectedOptionId)) {
            Log.w(TAG, "showQuestion() selectedOptionId=" + selectedOptionId
                    + " not found in current question pack (exam/question data may have changed)");
            isAnswered = false; // treat as unanswered for display
        }

        binding.tvUnansweredBadge.setVisibility(isAnswered ? View.GONE : View.VISIBLE);
        binding.tvQuestionText.setText(question.getQuestionText());

        buildOptionViews(question, isAnswered ? selectedOptionId : null);

        String explanation = question.getExplanation();
        if (explanation == null || explanation.trim().isEmpty()) {
            explanation = getString(R.string.review_explanation_placeholder);
        }
        binding.tvExplanation.setText(explanation);
    }

    /** Kiểm tra optionId có tồn tại trong danh sách options của question không. */
    private boolean optionExistsInQuestion(Question question, Long optionId) {
        if (question.getOptions() == null || optionId == null) return false;
        for (Question.Option opt : question.getOptions()) {
            if (optionId.equals(opt.getId())) return true;
        }
        return false;
    }

    private void buildOptionViews(Question question, Long selectedOptionId) {
        binding.llOptions.removeAllViews();
        if (question.getOptions() == null) return;

        for (Question.Option option : question.getOptions()) {
            boolean isThis = option.getId() != null && option.getId().equals(selectedOptionId);
            boolean isCorrect = option.isCorrect();

            MaterialCardView card = new MaterialCardView(this);
            card.setRadius(dpToPx(10));
            card.setCardElevation(dpToPx(1));
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = dpToPx(10);
            card.setLayoutParams(cardLp);

            if (isCorrect && isThis) {
                card.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
                card.setStrokeColor(ContextCompat.getColor(this, R.color.success));
                card.setStrokeWidth(dpToPx(2));
            } else if (isCorrect) {
                card.setCardBackgroundColor(Color.parseColor("#F1F8E9"));
                card.setStrokeColor(Color.parseColor("#AED581"));
                card.setStrokeWidth(dpToPx(2));
            } else if (isThis) {
                card.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
                card.setStrokeColor(Color.parseColor("#EF9A9A"));
                card.setStrokeWidth(dpToPx(2));
            } else {
                card.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
                card.setStrokeColor(Color.parseColor("#E0E0E0"));
                card.setStrokeWidth(dpToPx(1));
            }

            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.HORIZONTAL);
            inner.setGravity(Gravity.CENTER_VERTICAL);
            inner.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));

            TextView tvLabel = new TextView(this);
            String labelText = option.getOptionLabel() != null ? option.getOptionLabel() + "." : "";
            tvLabel.setText(labelText);
            tvLabel.setTextSize(14);
            tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            if (isCorrect) {
                tvLabel.setTextColor(ContextCompat.getColor(this, R.color.success));
            } else if (isThis) {
                tvLabel.setTextColor(Color.parseColor("#E53935"));
            } else {
                tvLabel.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            labelLp.rightMargin = dpToPx(8);
            tvLabel.setLayoutParams(labelLp);

            TextView tvText = new TextView(this);
            tvText.setText(option.getOptionText());
            tvText.setTextSize(14);
            tvText.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvText.setLayoutParams(textLp);

            inner.addView(tvLabel);
            inner.addView(tvText);

            if (isCorrect && isThis) {
                inner.addView(buildStatusIcon("✓", "#4CAF50"));
            } else if (isCorrect) {
                inner.addView(buildStatusLabel(getString(R.string.review_option_correct_label), "#4CAF50"));
            } else if (isThis) {
                LinearLayout statusCol = new LinearLayout(this);
                statusCol.setOrientation(LinearLayout.VERTICAL);
                statusCol.setGravity(Gravity.CENTER_HORIZONTAL);
                statusCol.addView(buildStatusIcon("✗", "#E53935"));
                statusCol.addView(buildStatusLabel(getString(R.string.review_option_yours_label), "#E53935"));
                inner.addView(statusCol);
            }

            card.addView(inner);
            binding.llOptions.addView(card);
        }
    }

    private TextView buildStatusIcon(String symbol, String colorHex) {
        TextView tv = new TextView(this);
        tv.setText(symbol);
        tv.setTextSize(18);
        tv.setTextColor(Color.parseColor(colorHex));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dpToPx(6);
        tv.setLayoutParams(lp);
        return tv;
    }

    private TextView buildStatusLabel(String text, String colorHex) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(11);
        tv.setTextColor(Color.parseColor(colorHex));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dpToPx(4);
        tv.setLayoutParams(lp);
        return tv;
    }

    private void navigate(int direction) {
        int next = currentIndex + direction;
        if (next >= 0 && next < questionIds.size()) {
            showQuestion(next);
        }
    }

    @SuppressLint("SetTextI18n")
    private void showQuestionGridDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_question_grid, null);

        TextView tvAnswered = dialogView.findViewById(R.id.tvAnsweredCount);
        TextView tvBookmarked = dialogView.findViewById(R.id.tvBookmarkedCount);
        int answeredCount = selectedAnswers.size();
        tvAnswered.setText(getString(R.string.review_grid_answered, answeredCount, questionIds.size()));
        tvBookmarked.setText(getString(R.string.review_grid_skipped, questionIds.size() - answeredCount));

        RecyclerView rvGrid = dialogView.findViewById(R.id.rvQuestionGrid);
        rvGrid.setLayoutManager(new GridLayoutManager(this, 7));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        ReviewGridAdapter adapter = new ReviewGridAdapter(
                questionIds, selectedAnswers, currentIndex,
                position -> {
                    dialog.dismiss();
                    showQuestion(position);
                });
        rvGrid.setAdapter(adapter);

        View btnSubmit = dialogView.findViewById(R.id.btnSubmitFromGrid);
        if (btnSubmit != null) btnSubmit.setVisibility(View.GONE);

        dialog.show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_INDEX, currentIndex);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
