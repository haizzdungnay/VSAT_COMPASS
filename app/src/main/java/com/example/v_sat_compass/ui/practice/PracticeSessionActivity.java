package com.example.v_sat_compass.ui.practice;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.PracticeGradeResponse;
import com.example.v_sat_compass.data.model.enums.Difficulty;
import com.example.v_sat_compass.data.model.session.QuestionOptionContentResponse;
import com.example.v_sat_compass.data.model.session.SessionQuestionContentResponse;
import com.example.v_sat_compass.data.repository.PracticeRepository;
import com.example.v_sat_compass.databinding.ActivityPracticeSessionBinding;
import com.example.v_sat_compass.util.KaTeXHelper;
import com.example.v_sat_compass.util.NetworkUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class PracticeSessionActivity extends AppCompatActivity {

    public static final String EXTRA_TOPIC_ID = "practice_topic_id";
    public static final String EXTRA_TOPIC_NAME = "practice_topic_name";

    private ActivityPracticeSessionBinding binding;
    private final AdaptiveDifficultyEngine difficultyEngine = new AdaptiveDifficultyEngine();
    private final PracticeRepository repository = new PracticeRepository();
    private Long topicId;
    private String topicName;
    private SessionQuestionContentResponse currentQuestion;
    private Long selectedOptionId;
    private boolean graded;
    private int total;
    private int correct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPracticeSessionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        topicId = getIntent().hasExtra(EXTRA_TOPIC_ID)
                ? getIntent().getLongExtra(EXTRA_TOPIC_ID, 0L)
                : null;
        if (topicId != null && topicId == 0L) topicId = null;
        topicName = getIntent().getStringExtra(EXTRA_TOPIC_NAME);

        binding.tvTitle.setText(topicName != null && !topicName.isEmpty() ? topicName : "Luyen thong minh");
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnEnd.setOnClickListener(v -> finish());
        binding.btnPrimary.setOnClickListener(v -> {
            if (graded) {
                loadNextQuestion();
            } else {
                submitCurrentAnswer();
            }
        });

        if (!NetworkUtils.isOnline(this)) {
            showState("Can co mang de luyen thong minh");
            binding.btnPrimary.setEnabled(false);
            return;
        }
        loadNextQuestion();
    }

    private void loadNextQuestion() {
        graded = false;
        selectedOptionId = null;
        currentQuestion = null;
        binding.cardResult.setVisibility(android.view.View.GONE);
        binding.btnPrimary.setEnabled(false);
        binding.btnPrimary.setText("Dang tai...");
        showState("Dang tai cau hoi...");
        binding.llOptions.removeAllViews();
        KaTeXHelper.renderOrText(binding.wvQuestionMath, binding.tvQuestion, "");
        KaTeXHelper.renderOrText(binding.wvExplanationMath, binding.tvExplanation, "");
        updateMeta();

        Difficulty difficulty = difficultyEngine.current();
        repository.loadQuestions(null, topicId, difficulty.name(), 1, new PracticeRepository.QuestionsCallback() {
            @Override
            public void onSuccess(List<SessionQuestionContentResponse> questions) {
                if (questions.isEmpty()) {
                    showState("Chua co cau hoi phu hop voi muc " + difficulty.name());
                    binding.btnPrimary.setText("Thu lai");
                    binding.btnPrimary.setEnabled(true);
                    graded = true;
                    return;
                }
                showQuestion(questions.get(0));
            }

            @Override
            public void onError(String message) {
                showState(message);
                binding.btnPrimary.setText("Thu lai");
                binding.btnPrimary.setEnabled(true);
                graded = true;
            }
        });
    }

    private void showQuestion(SessionQuestionContentResponse question) {
        currentQuestion = question;
        hideState();
        KaTeXHelper.renderOrText(binding.wvQuestionMath, binding.tvQuestion, question.getContent());
        binding.btnPrimary.setText("Chấm câu này");
        binding.btnPrimary.setEnabled(false);
        renderOptions(new ArrayList<>(), null);
        updateMeta();
    }

    private void submitCurrentAnswer() {
        if (currentQuestion == null || selectedOptionId == null) {
            Toast.makeText(this, "Chon mot dap an truoc", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.btnPrimary.setEnabled(false);
        binding.btnPrimary.setText("Đang chấm...");
        repository.grade(currentQuestion.getId(), selectedOptionId, new PracticeRepository.GradeCallback() {
            @Override
            public void onSuccess(PracticeGradeResponse result) {
                graded = true;
                total++;
                if (result.isCorrect()) correct++;
                difficultyEngine.recordAnswer(result.isCorrect());
                renderOptions(result.getCorrectOptionIds(), result.getSelectedOptionId());
                binding.cardResult.setVisibility(android.view.View.VISIBLE);
                binding.tvResult.setText(result.isCorrect() ? "Đúng" : "Chưa đúng");
                String explanation = result.getExplanation();
                String expText = explanation != null && !explanation.trim().isEmpty()
                        ? explanation
                        : "Chua co loi giai cho cau nay";
                KaTeXHelper.renderOrText(binding.wvExplanationMath, binding.tvExplanation, expText);
                binding.btnPrimary.setText("Cau tiep theo");
                binding.btnPrimary.setEnabled(true);
                updateMeta();
            }

            @Override
            public void onError(String message) {
        binding.btnPrimary.setText("Chấm câu này");
                binding.btnPrimary.setEnabled(true);
                Toast.makeText(PracticeSessionActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderOptions(List<Long> correctOptionIds, Long gradedSelectedOptionId) {
        binding.llOptions.removeAllViews();
        if (currentQuestion == null) return;
        List<QuestionOptionContentResponse> options = currentQuestion.getOptions();
        for (int i = 0; i < options.size(); i++) {
            QuestionOptionContentResponse option = options.get(i);
            MaterialCardView card = new MaterialCardView(this);
            card.setRadius(dpToPx(10));
            card.setCardElevation(dpToPx(1));
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = dpToPx(10);
            card.setLayoutParams(cardLp);

            Long activeSelection = gradedSelectedOptionId != null ? gradedSelectedOptionId : selectedOptionId;
            boolean selected = activeSelection != null && activeSelection.equals(option.getId());
            boolean correctAnswer = correctOptionIds.contains(option.getId());
            if (graded && correctAnswer) {
                card.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
                card.setStrokeColor(ContextCompat.getColor(this, R.color.success));
                card.setStrokeWidth(dpToPx(2));
            } else if (selected) {
                card.setCardBackgroundColor(Color.parseColor("#EEF2FF"));
                card.setStrokeColor(ContextCompat.getColor(this, R.color.primary));
                card.setStrokeWidth(dpToPx(2));
            } else {
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
                card.setStrokeColor(Color.parseColor("#E0E0E0"));
                card.setStrokeWidth(dpToPx(1));
            }

            TextView tv = new TextView(this);
            tv.setGravity(Gravity.CENTER_VERTICAL);
            tv.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
            tv.setText(String.valueOf((char) ('A' + i)) + ". " + option.getContent());
            tv.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            tv.setTextSize(14);
            card.addView(tv);
            card.setEnabled(!graded);
            card.setOnClickListener(v -> {
                selectedOptionId = option.getId();
                binding.btnPrimary.setEnabled(true);
                renderOptions(new ArrayList<>(), null);
            });
            binding.llOptions.addView(card);
        }
    }

    private void updateMeta() {
        binding.tvMeta.setText("Do kho: " + difficultyEngine.current().name());
        binding.tvScore.setText(correct + "/" + total);
    }

    private void showState(String text) {
        binding.tvState.setText(text);
        binding.tvState.setVisibility(android.view.View.VISIBLE);
    }

    private void hideState() {
        binding.tvState.setVisibility(android.view.View.GONE);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
