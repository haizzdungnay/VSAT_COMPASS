package com.example.v_sat_compass.ui.exam.session;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
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
import com.example.v_sat_compass.data.local.LocalExamDataSource;
import com.example.v_sat_compass.data.model.AiExplainResponse;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.data.model.Question;
import com.example.v_sat_compass.data.model.session.QuestionAnswerKeyResponse;
import com.example.v_sat_compass.data.model.session.QuestionOptionContentResponse;
import com.example.v_sat_compass.data.model.session.SessionAnswerKeysResponse;
import com.example.v_sat_compass.data.model.session.SessionQuestionContentResponse;
import com.example.v_sat_compass.data.repository.AiExplanationRepository;
import com.example.v_sat_compass.data.repository.OfflineExamRepository;
import com.example.v_sat_compass.data.repository.SessionContentRepository;
import com.example.v_sat_compass.data.repository.TicketRepository;
import com.example.v_sat_compass.databinding.ActivityExamReviewBinding;
import com.example.v_sat_compass.util.NetworkUtils;
import com.example.v_sat_compass.util.UserRoleHelper;
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
    private static final String BACKEND_REVIEW_PREFS = "vsat_backend_review";
    private static final String PREF_SESSION_PREFIX = "session_id_";
    private static final String PREF_QUESTION_IDS_PREFIX = "question_ids_";
    private static final String PREF_QUESTIONS_PREFIX = "questions_";

    /**
     * Per-user key so cached backend review snapshots (created by another
     * account on the same device) never leak into the current user and
     * trigger 403 SESSION_FORBIDDEN on the answer-keys endpoint.
     */
    private String perUserKey(String prefix, long examId) {
        return prefix + UserRoleHelper.getUserId() + "_" + examId;
    }

    public static final String EXTRA_EXAM_ID = "review_exam_id";
    public static final String EXTRA_SELECTED_ANSWERS_JSON = "review_selected_answers_json";

    private static final String KEY_CURRENT_INDEX = "review_current_index";

    private ActivityExamReviewBinding binding;

    private long examId;
    /** questionId → selectedOptionId; thiếu key = câu chưa làm */
    private Map<Long, Long> selectedAnswers = new HashMap<>();

    private List<Long> questionIds = new ArrayList<>();
    private int currentIndex = 0;
    private long backendSessionId;
    private boolean backendReviewMode;
    private SessionContentRepository sessionContentRepository;
    private OfflineExamRepository offlineExamRepository;
    private TicketRepository ticketRepository;
    private AiExplanationRepository aiExplanationRepository;
    private Map<Long, SessionQuestionContentResponse> backendQuestionById = new HashMap<>();
    private Map<Long, QuestionAnswerKeyResponse> backendAnswerKeyByQuestionId = new HashMap<>();

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

        sessionContentRepository = new SessionContentRepository();
        offlineExamRepository = OfflineExamRepository.getInstance();
        ticketRepository = new TicketRepository();
        aiExplanationRepository = new AiExplanationRepository();
        backendReviewMode = ApiClient.USE_BACKEND_EXAM_CONTENT && loadBackendReviewSnapshot();
        if (!backendReviewMode) {
            loadQuestionsFromLocal();
        }

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
        binding.btnReportQuestion.setOnClickListener(v -> showReportDialog());
        binding.btnAiExplain.setOnClickListener(v -> requestAiExplanation());

        if (backendReviewMode) {
            loadBackendAnswerKeys();
        } else {
            showQuestion(currentIndex);
        }
    }

    private boolean loadBackendReviewSnapshot() {
        SharedPreferences prefs = getSharedPreferences(BACKEND_REVIEW_PREFS, MODE_PRIVATE);
        backendSessionId = prefs.getLong(perUserKey(PREF_SESSION_PREFIX, examId), 0L);
        String questionIdsJson = prefs.getString(perUserKey(PREF_QUESTION_IDS_PREFIX, examId), null);
        String questionsJson = prefs.getString(perUserKey(PREF_QUESTIONS_PREFIX, examId), null);
        if (backendSessionId == 0L) {
            return false;
        }

        try {
            questionIds.clear();
            backendQuestionById.clear();

            if (questionIdsJson != null && !questionIdsJson.isEmpty()) {
                Type questionIdsType = new TypeToken<ArrayList<Long>>() {}.getType();
                List<Long> parsedQuestionIds = new Gson().fromJson(questionIdsJson, questionIdsType);
                if (parsedQuestionIds != null) {
                    questionIds.addAll(parsedQuestionIds);
                }
            }

            if (questionsJson != null && !questionsJson.isEmpty()) {
                Type type = new TypeToken<ArrayList<SessionQuestionContentResponse>>() {}.getType();
                List<SessionQuestionContentResponse> questions = new Gson().fromJson(questionsJson, type);
                if (questions != null) {
                    for (SessionQuestionContentResponse question : questions) {
                        Long questionId = question.getId();
                        if (!questionIds.contains(questionId)) {
                            questionIds.add(questionId);
                        }
                        backendQuestionById.put(questionId, question);
                    }
                }
            }
            return !questionIds.isEmpty();
        } catch (JsonSyntaxException e) {
            Log.w(TAG, "loadBackendReviewSnapshot() failed to parse backend questions", e);
            return false;
        }
    }

    private void loadBackendAnswerKeys() {
        sessionContentRepository.getAnswerKeys(backendSessionId,
                new SessionContentRepository.RepositoryCallback<SessionAnswerKeysResponse>() {
                    @Override
                    public void onSuccess(SessionAnswerKeysResponse data) {
                        backendAnswerKeyByQuestionId.clear();
                        for (QuestionAnswerKeyResponse question : data.getQuestions()) {
                            backendAnswerKeyByQuestionId.put(question.getQuestionId(), question);
                        }
                        showQuestion(currentIndex);
                    }

                    @Override
                    public void onError(SessionContentRepository.SessionContentError error) {
                        // 403 SESSION_FORBIDDEN = the cached sessionId belongs to
                        // another user (e.g. account switch on the same device, or
                        // a student submitted then admin reviewed). Wipe the stale
                        // snapshot and fall back to local review so the screen
                        // remains usable instead of a misleading "check network" toast.
                        if ("SESSION_FORBIDDEN".equals(error.getCode())
                                || error.getStatusCode() == 403) {
                            Log.w(TAG, "loadBackendAnswerKeys() cached session "
                                    + backendSessionId + " not owned by current user, "
                                    + "dropping snapshot and falling back to local review");
                            clearBackendReviewSnapshot();
                            backendSessionId = 0L;
                            backendReviewMode = false;
                            loadQuestionsFromLocal();
                            showQuestion(currentIndex);
                            return;
                        }
                        Toast.makeText(ExamReviewActivity.this,
                                "Khong tai duoc dap an -- kiem tra mang",
                                Toast.LENGTH_SHORT).show();
                        showQuestion(currentIndex);
                    }
                });
    }

    private void clearBackendReviewSnapshot() {
        SharedPreferences prefs = getSharedPreferences(BACKEND_REVIEW_PREFS, MODE_PRIVATE);
        prefs.edit()
                .remove(perUserKey(PREF_SESSION_PREFIX, examId))
                .remove(perUserKey(PREF_QUESTION_IDS_PREFIX, examId))
                .remove(perUserKey(PREF_QUESTIONS_PREFIX, examId))
                .apply();
    }

    private void loadQuestionsFromLocal() {
        List<Long> downloadedIds = offlineExamRepository.loadQuestionIds(this, examId);
        if (!downloadedIds.isEmpty()) {
            questionIds.addAll(downloadedIds);
            return;
        }
        Exam exam = LocalExamDataSource.getInstance().getExamDetail(this, examId);
        if (exam != null && exam.getQuestions() != null) {
            for (Exam.ExamQuestion eq : exam.getQuestions()) {
                questionIds.add(eq.getQuestionId());
            }
        }
    }

    private void showQuestion(int index) {
        if (backendReviewMode) {
            showBackendQuestion(index);
            return;
        }

        if (index < 0 || index >= questionIds.size()) return;
        currentIndex = index;

        Long questionId = questionIds.get(index);
        resetAiExplanationState();
        Question question = offlineExamRepository.isDownloaded(this, examId)
                ? offlineExamRepository.loadQuestion(this, examId, questionId)
                : LocalExamDataSource.getInstance().getQuestion(this, questionId);

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
    private void showBackendQuestion(int index) {
        if (index < 0 || index >= questionIds.size()) return;
        currentIndex = index;

        Long questionId = questionIds.get(index);
        resetAiExplanationState();
        SessionQuestionContentResponse question = backendQuestionById.get(questionId);

        int total = questionIds.size();

        binding.tvQuestionCounter.setText(getString(R.string.review_counter_format, index + 1, total));
        binding.tvQuestionBadge.setText(getString(R.string.review_question_label, index + 1));
        int progress = (int) (((index + 1) * 100.0) / total);
        binding.progressReview.setProgress(progress);

        binding.scrollContent.smoothScrollTo(0, 0);

        binding.btnPrevious.setEnabled(index > 0);
        binding.btnNext.setEnabled(index < total - 1);

        if (question == null) {
            loadMissingBackendQuestion(questionId, index);
            return;
        }

        Long selectedOptionId = selectedAnswers.get(questionId);
        boolean isAnswered = selectedOptionId != null;
        if (isAnswered && !optionExistsInBackendQuestion(question, selectedOptionId)) {
            Log.w(TAG, "showBackendQuestion() selectedOptionId=" + selectedOptionId
                    + " not found in backend question content");
            isAnswered = false;
        }

        binding.tvUnansweredBadge.setVisibility(isAnswered ? View.GONE : View.VISIBLE);
        binding.tvQuestionText.setText(question.getContent());

        buildBackendOptionViews(question, isAnswered ? selectedOptionId : null);

        QuestionAnswerKeyResponse answerKey = backendAnswerKeyByQuestionId.get(questionId);
        String explanation = answerKey != null ? answerKey.getExplanation() : null;
        if (explanation == null || explanation.trim().isEmpty()) {
            explanation = getString(R.string.review_explanation_placeholder);
        }
        binding.tvExplanation.setText(explanation);
    }

    private boolean optionExistsInBackendQuestion(
            SessionQuestionContentResponse question,
            Long optionId
    ) {
        if (question.getOptions() == null || optionId == null) return false;
        for (QuestionOptionContentResponse opt : question.getOptions()) {
            if (optionId.equals(opt.getId())) return true;
        }
        return false;
    }

    private void loadMissingBackendQuestion(Long questionId, int index) {
        sessionContentRepository.getQuestion(backendSessionId, questionId,
                new SessionContentRepository.RepositoryCallback<SessionQuestionContentResponse>() {
                    @Override
                    public void onSuccess(SessionQuestionContentResponse data) {
                        backendQuestionById.put(questionId, data);
                        showBackendQuestion(index);
                    }

                    @Override
                    public void onError(SessionContentRepository.SessionContentError error) {
                        Toast.makeText(ExamReviewActivity.this,
                                "Khong tai duoc cau hoi -- kiem tra mang",
                                Toast.LENGTH_SHORT).show();
                        binding.tvQuestionText.setText(getString(R.string.review_question_unavailable));
                        binding.tvUnansweredBadge.setVisibility(View.GONE);
                        binding.llOptions.removeAllViews();
                        binding.tvExplanation.setText(getString(R.string.review_explanation_placeholder));
                    }
                });
    }

    private String resolveQuestionType(Question q, SessionQuestionContentResponse bq) {
        if (q != null && q.getQuestionType() != null && !q.getQuestionType().isEmpty()) {
            return q.getQuestionType().toUpperCase(java.util.Locale.ROOT);
        }
        if (bq != null && bq.getQuestionType() != null && !bq.getQuestionType().isEmpty()) {
            return bq.getQuestionType().toUpperCase(java.util.Locale.ROOT);
        }
        String code = q != null ? q.getQuestionCode() : (bq != null ? bq.getQuestionCode() : "");
        if (code != null) {
            String upper = code.toUpperCase(java.util.Locale.ROOT);
            if (upper.contains("TF") || upper.contains("TRUE_FALSE")) return "TRUE_FALSE";
            if (upper.contains("MATCH")) return "MATCHING";
            if (upper.contains("SA") || upper.contains("SHORT")) return "SHORT_ANSWER";
        }
        return "SINGLE_CHOICE";
    }

    private View createFormatBadge(String title, int textColor, int bgColor) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextSize(12);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setTextColor(textColor);
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(dpToPx(6));
        tv.setBackground(gd);
        tv.setPadding(dpToPx(10), dpToPx(5), dpToPx(10), dpToPx(5));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dpToPx(12);
        tv.setLayoutParams(lp);
        return tv;
    }

    private void buildBackendOptionViews(
            SessionQuestionContentResponse question,
            Long selectedOptionId
    ) {
        binding.llOptions.removeAllViews();
        List<QuestionOptionContentResponse> options = question.getOptions();
        if (options == null) return;

        String bqType = resolveQuestionType(null, question);
        if ("TRUE_FALSE".equalsIgnoreCase(bqType)) {
            binding.llOptions.addView(createFormatBadge("PHẦN 1: CÂU HỎI ĐÚNG / SAI (4 Ý)", 0xFF006064, 0xFFE0F7FA));
        } else if ("MATCHING".equalsIgnoreCase(bqType)) {
            binding.llOptions.addView(createFormatBadge("PHẦN 3: GHÉP HỢP (NỐI CẶP)", 0xFFE65100, 0xFFFFF3E0));
        } else if ("SHORT_ANSWER".equalsIgnoreCase(bqType) || "FILL_IN_BLANK".equalsIgnoreCase(bqType)) {
            binding.llOptions.addView(createFormatBadge("PHẦN 4: ĐIỀN SỐ / TRẢ LỜI NGẮN", 0xFF1B5E20, 0xFFE8F5E9));
        } else {
            binding.llOptions.addView(createFormatBadge("PHẦN 2: TRẮC NGHIỆM 4 LỰA CHỌN", 0xFF283593, 0xFFE8EAF6));
        }

        QuestionAnswerKeyResponse answerKey = backendAnswerKeyByQuestionId.get(question.getId());
        List<Long> correctOptionIds = answerKey != null
                ? answerKey.getCorrectOptionIds()
                : new ArrayList<>();

        for (int i = 0; i < options.size(); i++) {
            QuestionOptionContentResponse option = options.get(i);
            Long optionId = option.getId();
            boolean isThis = optionId.equals(selectedOptionId);
            boolean isCorrect = correctOptionIds.contains(optionId);

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
            tvLabel.setText(String.valueOf((char) ('A' + i)) + ".");
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
            tvText.setText(option.getContent());
            tvText.setTextSize(14);
            tvText.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvText.setLayoutParams(textLp);

            inner.addView(tvLabel);
            inner.addView(tvText);

            if (isCorrect && isThis) {
                inner.addView(buildStatusIcon("OK", "#4CAF50"));
            } else if (isCorrect) {
                inner.addView(buildStatusLabel(getString(R.string.review_option_correct_label), "#4CAF50"));
            } else if (isThis) {
                LinearLayout statusCol = new LinearLayout(this);
                statusCol.setOrientation(LinearLayout.VERTICAL);
                statusCol.setGravity(Gravity.CENTER_HORIZONTAL);
                statusCol.addView(buildStatusIcon("X", "#E53935"));
                statusCol.addView(buildStatusLabel(getString(R.string.review_option_yours_label), "#E53935"));
                inner.addView(statusCol);
            }

            card.addView(inner);
            binding.llOptions.addView(card);
        }
    }

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

        String qType = resolveQuestionType(question, null);
        if ("TRUE_FALSE".equalsIgnoreCase(qType)) {
            binding.llOptions.addView(createFormatBadge("PHẦN 1: CÂU HỎI ĐÚNG / SAI (4 Ý)", 0xFF006064, 0xFFE0F7FA));
        } else if ("MATCHING".equalsIgnoreCase(qType)) {
            binding.llOptions.addView(createFormatBadge("PHẦN 3: GHÉP HỢP (NỐI CẶP)", 0xFFE65100, 0xFFFFF3E0));
        } else if ("SHORT_ANSWER".equalsIgnoreCase(qType) || "FILL_IN_BLANK".equalsIgnoreCase(qType)) {
            binding.llOptions.addView(createFormatBadge("PHẦN 4: ĐIỀN SỐ / TRẢ LỜI NGẮN", 0xFF1B5E20, 0xFFE8F5E9));
        } else {
            binding.llOptions.addView(createFormatBadge("PHẦN 2: TRẮC NGHIỆM 4 LỰA CHỌN", 0xFF283593, 0xFFE8EAF6));
        }

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

    private void resetAiExplanationState() {
        binding.cardAiExplanation.setVisibility(View.GONE);
        binding.tvAiExplanation.setText("");
        binding.btnAiExplain.setEnabled(true);
        binding.btnAiExplain.setText("Giai thich them");
    }

    private void requestAiExplanation() {
        if (currentIndex < 0 || currentIndex >= questionIds.size()) return;
        if (!NetworkUtils.isOnline(this)) {
            binding.cardAiExplanation.setVisibility(View.VISIBLE);
            binding.tvAiExplanation.setText("Can co mang de dung AI giai thich");
            return;
        }
        Long questionId = questionIds.get(currentIndex);
        Long selectedOptionId = selectedAnswers.get(questionId);
        binding.cardAiExplanation.setVisibility(View.VISIBLE);
        binding.tvAiExplanation.setText("Dang goi AI...");
        binding.btnAiExplain.setEnabled(false);
        binding.btnAiExplain.setText("Dang giai thich...");
        aiExplanationRepository.explain(questionId, selectedOptionId, new AiExplanationRepository.ExplainCallback() {
            @Override
            public void onSuccess(AiExplainResponse response) {
                if (binding == null) return;
                if (currentIndex < 0 || currentIndex >= questionIds.size()
                        || !questionId.equals(questionIds.get(currentIndex))) return;
                binding.tvAiExplanation.setText(response.getExplanation() != null && !response.getExplanation().trim().isEmpty()
                        ? response.getExplanation()
                        : "AI chua co noi dung giai thich");
                binding.btnAiExplain.setEnabled(true);
                binding.btnAiExplain.setText(response.isCached() ? "Giai thich da cache" : "Giai thich them");
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                if (currentIndex < 0 || currentIndex >= questionIds.size()
                        || !questionId.equals(questionIds.get(currentIndex))) return;
                binding.tvAiExplanation.setText(message);
                binding.btnAiExplain.setEnabled(true);
                binding.btnAiExplain.setText("Thu lai AI");
            }
        });
    }

    private void showReportDialog() {
        if (currentIndex < 0 || currentIndex >= questionIds.size()) return;
        Long questionId = questionIds.get(currentIndex);
        EditText input = new EditText(this);
        input.setHint("Mo ta loi dap an/noi dung");
        input.setMinLines(3);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        int pad = dpToPx(16);
        input.setPadding(pad, dpToPx(8), pad, 0);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Bao loi cau hoi")
                .setView(input)
                .setPositiveButton("Gui", null)
                .setNegativeButton("Hủy", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String message = input.getText().toString().trim();
            if (message.isEmpty()) {
                input.setError("Nhap mo ta loi");
                return;
            }
            submitReport(questionId, message, dialog);
        }));
        dialog.show();
    }

    private void submitReport(Long questionId, String message, AlertDialog dialog) {
        binding.btnReportQuestion.setEnabled(false);
        ticketRepository.createTicket(questionId, message, new TicketRepository.TicketCallback() {
            @Override
            public void onSuccess(com.example.v_sat_compass.data.model.Ticket ticket) {
                if (binding == null) return;
                binding.btnReportQuestion.setEnabled(true);
                dialog.dismiss();
                Toast.makeText(ExamReviewActivity.this, "Da gui bao loi", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.btnReportQuestion.setEnabled(true);
                Toast.makeText(ExamReviewActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
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
