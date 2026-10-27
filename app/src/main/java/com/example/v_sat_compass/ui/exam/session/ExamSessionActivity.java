package com.example.v_sat_compass.ui.exam.session;

import android.content.Intent;
import android.content.SharedPreferences;
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
import com.example.v_sat_compass.data.model.ClientSubmitRequest;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.data.model.ExamHistoryEntry;
import com.example.v_sat_compass.data.model.ExamSession;
import com.example.v_sat_compass.data.model.Question;
import com.example.v_sat_compass.data.model.session.QuestionOptionContentResponse;
import com.example.v_sat_compass.data.model.session.SessionQuestionContentResponse;
import com.example.v_sat_compass.data.repository.ExamHistoryRepository;
import com.example.v_sat_compass.data.repository.GamificationRepository;
import com.example.v_sat_compass.data.repository.OfflineExamRepository;
import com.example.v_sat_compass.data.repository.OfflineExamSyncRepository;
import com.example.v_sat_compass.data.repository.SessionContentRepository;
import com.example.v_sat_compass.databinding.ActivityExamSessionBinding;
import com.example.v_sat_compass.util.UserRoleHelper;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;

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
    private static final String BACKEND_REVIEW_PREFS = "vsat_backend_review";
    private static final String PREF_SESSION_PREFIX = "session_id_";
    private static final String PREF_QUESTION_IDS_PREFIX = "question_ids_";
    private static final String PREF_QUESTIONS_PREFIX = "questions_";

    /**
     * Build a per-user pref key so cached backend session/question snapshots
     * never leak across accounts on the same device (admin vs student).
     */
    private static String perUserKey(String prefix, long examId) {
        long userId = UserRoleHelper.getUserId();
        return prefix + userId + "_" + examId;
    }

    private ActivityExamSessionBinding binding;
    private ExamApi examApi;
    private SessionContentRepository sessionContentRepository;
    private OfflineExamRepository offlineExamRepository;

    private Long sessionId;
    private long examId;
    private String examTitle;
    private String examSubject;
    private int durationMinutes;
    private int totalQuestions;

    private List<Long> questionIds = new ArrayList<>();
    private int currentIndex = 0;
    private Question currentQuestion;
    private SessionQuestionContentResponse currentBackendQuestion;
    private Map<Long, Long> selectedAnswers = new HashMap<>();
    private final Map<Long, String> shortAnswers = new HashMap<>();
    private final Map<Long, Map<String, Boolean>> trueFalseAnswers = new HashMap<>();
    private final Map<Long, Map<String, String>> matchingAnswers = new HashMap<>();
    private Set<Long> bookmarkedQuestions = new HashSet<>();
    // Cache questions fetched from API so we can score locally without extra requests
    private Map<Long, Question> questionCache = new HashMap<>();
    private Map<Long, SessionQuestionContentResponse> backendQuestionCache = new HashMap<>();
    private boolean offlineDownloadedSession;
    private int offlineQuestionLoadGeneration;

    private CountDownTimer timer;
    private long sessionStartMillis;

    // Colors for option cards
    private int colorDefault;
    private int colorSelected;
    private int strokeDefault;
    private int strokeSelected;
    private final boolean clientSideProcessing = ApiClient.isClientSideExamProcessingEnabled();
    private final boolean backendExamContent = ApiClient.USE_BACKEND_EXAM_CONTENT;
    private boolean hasRemoteSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExamSessionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        examApi = ApiClient.getClient().create(ExamApi.class);
        sessionContentRepository = new SessionContentRepository();
        offlineExamRepository = OfflineExamRepository.getInstance();

        colorDefault = Color.WHITE;
        colorSelected = ContextCompat.getColor(this, R.color.answer_selected);
        strokeDefault = ContextCompat.getColor(this, R.color.text_hint);
        strokeSelected = ContextCompat.getColor(this, R.color.primary);

        examId = getIntent().getLongExtra("exam_id", 0);
        examTitle = getIntent().getStringExtra("exam_title");
        examSubject = getIntent().getStringExtra("exam_subject");
        durationMinutes = getIntent().getIntExtra("duration_minutes", 60);
        totalQuestions = getIntent().getIntExtra("total_questions", 0);

        String displayTitle = examTitle != null ? examTitle : "Đề thi";
        binding.tvExamTitle.setText(displayTitle);
        updateSyncStatus(false);

        binding.btnBack.setOnClickListener(v -> confirmExit());
        binding.btnPrevious.setOnClickListener(v -> navigateQuestion(-1));
        binding.btnNext.setOnClickListener(v -> navigateQuestion(1));
        binding.btnGrid.setOnClickListener(v -> showQuestionGrid());
        binding.btnBookmark.setOnClickListener(v -> toggleBookmark());

        startSession();
    }

    private void startSession() {
        sessionStartMillis = System.currentTimeMillis();

        if (offlineExamRepository.isDownloaded(this, examId)) {
            startOfflineDownloadedSession();
            return;
        }

        if (backendExamContent) {
            startBackendContentSession();
            return;
        }

        if (clientSideProcessing) {
            startLocalSession();
            tryBootstrapRemoteSession();
            return;
        }

        Map<String, Long> body = new HashMap<>();
        body.put("examId", examId);

        examApi.startSession(body).enqueue(new Callback<ApiResponse<ExamSession>>() {
            @Override
            public void onResponse(Call<ApiResponse<ExamSession>> call, Response<ApiResponse<ExamSession>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    ExamSession session = response.body().getData();
                    sessionId = session.getId();
                    hasRemoteSession = true;
                    questionIds.clear();
                    questionIds.addAll(session.getOrderedQuestionIds());
                    if (questionIds.isEmpty()) {
                        loadExamDetailFromLocal();
                        return;
                    }
                    totalQuestions = questionIds.size();
                    loadQuestion(0);
                    startTimer();
                } else {
                    Toast.makeText(ExamSessionActivity.this, "Không thể bắt đầu bài thi", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ExamSession>> call, Throwable t) {
                startLocalSession();
            }
        });
    }

    private void startBackendContentSession() {
        Map<String, Long> body = new HashMap<>();
        body.put("examId", examId);

        examApi.startSession(body).enqueue(new Callback<ApiResponse<ExamSession>>() {
            @Override
            public void onResponse(Call<ApiResponse<ExamSession>> call,
                                   Response<ApiResponse<ExamSession>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    ExamSession session = response.body().getData();
                    sessionId = session.getId();
                    hasRemoteSession = true;
                    updateSyncStatus(true);
                    questionIds.clear();
                    // Backend v0.10.2 returns orderedQuestionIds for content fetch order.
                    questionIds.addAll(session.getOrderedQuestionIds());
                    totalQuestions = questionIds.size();
                    android.util.Log.d("ExamSession",
                            "startBackendContentSession OK sessionId=" + sessionId
                                    + " questionIds=" + questionIds);
                    if (questionIds.isEmpty()) {
                        showEmptyExamAndExit("Đề thi này hiện chưa có câu hỏi nào.");
                        return;
                    }
                    loadQuestion(0);
                    startTimer();
                } else {
                    String bodyMsg = response.body() != null ? response.body().getMessage() : "null";
                    android.util.Log.w("ExamSession",
                            "startBackendContentSession failed code=" + response.code()
                                    + " bodyMsg=" + bodyMsg);
                    Toast.makeText(ExamSessionActivity.this,
                            "Khong the bat dau bai thi backend", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ExamSession>> call, Throwable t) {
                Toast.makeText(ExamSessionActivity.this,
                        "Khong the bat dau bai thi backend", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void startLocalSession() {
        hasRemoteSession = false;
        sessionId = System.currentTimeMillis();
        updateSyncStatus(false);
        loadExamDetailFromLocal();
        startTimer();
    }

    private void startOfflineDownloadedSession() {
        offlineDownloadedSession = true;
        hasRemoteSession = false;
        sessionId = System.currentTimeMillis();
        updateSyncStatus(false);
        loadExamDetailFromDownloadedAsync(true);
    }

    private void tryBootstrapRemoteSession() {
        Map<String, Long> body = new HashMap<>();
        body.put("examId", examId);

        examApi.startSession(body).enqueue(new Callback<ApiResponse<ExamSession>>() {
            @Override
            public void onResponse(Call<ApiResponse<ExamSession>> call, Response<ApiResponse<ExamSession>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    ExamSession remoteSession = response.body().getData();
                    if (remoteSession != null && remoteSession.getId() != null) {
                        sessionId = remoteSession.getId();
                        hasRemoteSession = true;
                        updateSyncStatus(true);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ExamSession>> call, Throwable t) {
                // Keep local-only exam flow when backend is unavailable.
                updateSyncStatus(false);
            }
        });
    }

    private void updateSyncStatus(boolean onlineSyncEnabled) {
        if (onlineSyncEnabled) {
            binding.tvSyncStatus.setText("Sync: online enabled");
            binding.tvSyncStatus.setTextColor(ContextCompat.getColor(this, R.color.primary));
        } else {
            binding.tvSyncStatus.setText("Sync: local-only");
            binding.tvSyncStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void loadExamDetailFromLocal() {
        if (offlineDownloadedSession) {
            loadExamDetailFromDownloadedAsync(false);
            return;
        }
        Exam exam = LocalExamDataSource.getInstance().getExamDetail(this, examId);
        if (exam == null || exam.getQuestions() == null || exam.getQuestions().isEmpty()) {
            showEmptyExamAndExit("Không có dữ liệu đề thi cục bộ");
            return;
        }

        questionIds.clear();
        for (Exam.ExamQuestion q : exam.getQuestions()) {
            questionIds.add(q.getQuestionId());
        }
        if (questionIds.isEmpty()) {
            showEmptyExamAndExit("Đề thi này hiện chưa có câu hỏi nào.");
            return;
        }
        totalQuestions = questionIds.size();
        loadQuestion(0);
    }

    private void loadExamDetailFromDownloadedAsync(boolean startTimerAfterLoad) {
        binding.tvQuestionText.setText("Dang tai de offline...");
        binding.llOptions.removeAllViews();
        offlineExamRepository.loadQuestionIds(this, examId, downloadedQuestionIds -> {
            if (binding == null || isFinishing() || isDestroyed()) return;
            if (downloadedQuestionIds == null || downloadedQuestionIds.isEmpty()) {
                showEmptyExamAndExit("De offline khong co cau hoi nao.");
                return;
            }
            questionIds.clear();
            questionIds.addAll(downloadedQuestionIds);
            totalQuestions = questionIds.size();
            loadQuestion(0);
            if (startTimerAfterLoad) {
                startTimer();
            }
        });
    }

    private void loadQuestion(int index) {
        // Defensive guard: never divide by zero or index out of an empty list.
        if (questionIds == null || questionIds.isEmpty() || totalQuestions <= 0) {
            showEmptyExamAndExit("Đề thi này hiện chưa có câu hỏi nào.");
            return;
        }
        if (index < 0 || index >= questionIds.size()) return;

        currentIndex = index;
        Long questionId = questionIds.get(index);

        binding.tvQuestionNumber.setText("Câu " + (index + 1) + "/" + totalQuestions);
        int progressPercent = (int) (((index + 1) * 100.0) / totalQuestions);
        binding.progressQuestion.setProgress(progressPercent);

        binding.btnPrevious.setEnabled(index > 0);
        binding.btnNext.setText(index == totalQuestions - 1 ? "Nộp bài" : "Tiếp theo");

        if (backendExamContent && hasRemoteSession) {
            loadBackendQuestion(questionId);
            return;
        }

        // Serve from cache first to avoid redundant network calls
        if (questionCache.containsKey(questionId)) {
            currentQuestion = questionCache.get(questionId);
            displayQuestion(currentQuestion);
            updateBookmarkIcon();
            return;
        }

        if (clientSideProcessing || !hasRemoteSession) {
            if (offlineDownloadedSession) {
                loadDownloadedQuestionAsync(index, questionId);
                return;
            }
            Question localQuestion = LocalExamDataSource.getInstance().getQuestion(this, questionId);
            if (localQuestion != null) {
                currentQuestion = localQuestion;
                questionCache.put(questionId, localQuestion);
                displayQuestion(currentQuestion);
                updateBookmarkIcon();
            } else {
                Toast.makeText(this, "Không tìm thấy câu hỏi cục bộ", Toast.LENGTH_SHORT).show();
            }
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

    private void loadDownloadedQuestionAsync(int requestedIndex, Long questionId) {
        int generation = ++offlineQuestionLoadGeneration;
        currentQuestion = null;
        binding.tvQuestionText.setText("Dang tai cau hoi offline...");
        binding.llOptions.removeAllViews();
        offlineExamRepository.loadQuestion(this, examId, questionId, localQuestion -> {
            if (binding == null || generation != offlineQuestionLoadGeneration
                    || requestedIndex != currentIndex || questionIds == null
                    || currentIndex < 0 || currentIndex >= questionIds.size()
                    || !questionId.equals(questionIds.get(currentIndex))) {
                return;
            }
            if (localQuestion != null) {
                currentQuestion = localQuestion;
                questionCache.put(questionId, localQuestion);
                displayQuestion(currentQuestion);
                updateBookmarkIcon();
            } else {
                Toast.makeText(this, "Khong tim thay cau hoi offline", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Show a clear error dialog when the exam has no questions, then close the activity.
     * Prevents the black-screen divide-by-zero crash and guides the user back to the
     * previous screen.
     */
    private void showEmptyExamAndExit(String message) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        new AlertDialog.Builder(this)
                .setTitle("Không thể mở đề thi")
                .setMessage(message != null ? message : "Đề thi này hiện chưa có câu hỏi nào.")
                .setCancelable(false)
                .setPositiveButton("Quay lại", (d, w) -> finish())
                .show();
    }

    private void loadBackendQuestion(Long questionId) {
        currentQuestion = null;
        if (sessionId == null) {
            Toast.makeText(this, "Khong co backend session", Toast.LENGTH_SHORT).show();
            binding.tvQuestionText.setText("(Khong co backend session)");
            return;
        }

        if (backendQuestionCache.containsKey(questionId)) {
            currentBackendQuestion = backendQuestionCache.get(questionId);
            displayBackendQuestion(currentBackendQuestion);
            updateBookmarkIcon();
            return;
        }

        android.util.Log.d("ExamSession",
                "loadBackendQuestion sessionId=" + sessionId + " questionId=" + questionId);
        sessionContentRepository.getQuestion(sessionId, questionId,
                new SessionContentRepository.RepositoryCallback<SessionQuestionContentResponse>() {
                    @Override
                    public void onSuccess(SessionQuestionContentResponse data) {
                        currentBackendQuestion = data;
                        backendQuestionCache.put(questionId, data);
                        displayBackendQuestion(data);
                        updateBookmarkIcon();
                    }

                    @Override
                    public void onError(SessionContentRepository.SessionContentError error) {
                        String message = error != null && error.getMessage() != null
                                ? error.getMessage()
                                : "Loi tai cau hoi backend";
                        android.util.Log.w("ExamSession",
                                "loadBackendQuestion failed q=" + questionId
                                        + " status=" + (error != null ? error.getStatusCode() : -1)
                                        + " code=" + (error != null ? error.getCode() : null)
                                        + " msg=" + message);
                        Toast.makeText(ExamSessionActivity.this, message, Toast.LENGTH_SHORT).show();
                        binding.tvQuestionText.setText("(Loi tai cau hoi: " + message + ")");
                    }
                });
    }

    private String resolveQuestionType(Question q, SessionQuestionContentResponse bq) {
        if (q != null && q.getQuestionType() != null && !q.getQuestionType().isEmpty()) {
            return q.getQuestionType().toUpperCase(Locale.ROOT);
        }
        if (bq != null && bq.getQuestionType() != null && !bq.getQuestionType().isEmpty()) {
            return bq.getQuestionType().toUpperCase(Locale.ROOT);
        }
        String code = q != null ? q.getQuestionCode() : (bq != null ? bq.getQuestionCode() : "");
        if (code != null) {
            String upper = code.toUpperCase(Locale.ROOT);
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

    private void renderTrueFalseWidget(Long questionId, List<Question.Option> opts) {
        binding.llOptions.addView(createFormatBadge(
                "PHẦN 1: CÂU HỎI ĐÚNG / SAI (4 Ý)", 0xFF006064, 0xFFE0F7FA));

        Map<String, Boolean> currentTf = trueFalseAnswers.computeIfAbsent(questionId, k -> new HashMap<>());
        if (opts == null || opts.isEmpty()) return;

        for (int i = 0; i < opts.size(); i++) {
            Question.Option opt = opts.get(i);
            String label = opt.getOptionLabel() != null ? opt.getOptionLabel() : String.valueOf((char) ('a' + i));
            String propKey = label.toLowerCase(Locale.ROOT);
            Boolean currentVal = currentTf.get(propKey);

            MaterialCardView card = new MaterialCardView(this);
            card.setRadius(dpToPx(8));
            card.setCardElevation(dpToPx(1));
            card.setStrokeWidth(dpToPx(1));
            card.setStrokeColor(0xFFE0E0E0);
            card.setCardBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = dpToPx(8);
            card.setLayoutParams(cardLp);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));

            TextView tvText = new TextView(this);
            tvText.setText(label + ") " + (opt.getOptionText() != null ? opt.getOptionText() : ""));
            tvText.setTextSize(13);
            tvText.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvText.setLayoutParams(textLp);
            row.addView(tvText);

            TextView btnDung = new TextView(this);
            btnDung.setText("Đúng");
            btnDung.setTextSize(12);
            btnDung.setGravity(Gravity.CENTER);
            btnDung.setTypeface(null, android.graphics.Typeface.BOLD);
            boolean isDung = Boolean.TRUE.equals(currentVal);
            btnDung.setTextColor(isDung ? Color.WHITE : 0xFF2E7D32);
            android.graphics.drawable.GradientDrawable gdDung = new android.graphics.drawable.GradientDrawable();
            gdDung.setColor(isDung ? 0xFF2E7D32 : 0xFFE8F5E9);
            gdDung.setCornerRadius(dpToPx(6));
            btnDung.setBackground(gdDung);
            btnDung.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
            LinearLayout.LayoutParams dungLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            dungLp.rightMargin = dpToPx(6);
            btnDung.setLayoutParams(dungLp);
            Long optId = opt.getId();
            btnDung.setOnClickListener(v -> {
                currentTf.put(propKey, true);
                selectedAnswers.put(questionId, optId != null ? optId : (questionId * 10 + 1));
                submitAnswer(questionId, selectedAnswers.get(questionId));
                displayQuestion(currentQuestion);
            });
            row.addView(btnDung);

            TextView btnSai = new TextView(this);
            btnSai.setText("Sai");
            btnSai.setTextSize(12);
            btnSai.setGravity(Gravity.CENTER);
            btnSai.setTypeface(null, android.graphics.Typeface.BOLD);
            boolean isSai = Boolean.FALSE.equals(currentVal);
            btnSai.setTextColor(isSai ? Color.WHITE : 0xFFC62828);
            android.graphics.drawable.GradientDrawable gdSai = new android.graphics.drawable.GradientDrawable();
            gdSai.setColor(isSai ? 0xFFC62828 : 0xFFFFEBEE);
            gdSai.setCornerRadius(dpToPx(6));
            btnSai.setBackground(gdSai);
            btnSai.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
            btnSai.setOnClickListener(v -> {
                currentTf.put(propKey, false);
                selectedAnswers.put(questionId, optId != null ? optId : (questionId * 10 + 1));
                submitAnswer(questionId, selectedAnswers.get(questionId));
                displayQuestion(currentQuestion);
            });
            row.addView(btnSai);

            card.addView(row);
            binding.llOptions.addView(card);
        }
    }

    private void renderBackendTrueFalseWidget(Long questionId, List<QuestionOptionContentResponse> opts) {
        binding.llOptions.addView(createFormatBadge(
                "PHẦN 1: CÂU HỎI ĐÚNG / SAI (4 Ý)", 0xFF006064, 0xFFE0F7FA));

        Map<String, Boolean> currentTf = trueFalseAnswers.computeIfAbsent(questionId, k -> new HashMap<>());
        if (opts == null || opts.isEmpty()) return;

        for (int i = 0; i < opts.size(); i++) {
            QuestionOptionContentResponse opt = opts.get(i);
            String label = String.valueOf((char) ('a' + i));
            String propKey = label.toLowerCase(Locale.ROOT);
            Boolean currentVal = currentTf.get(propKey);

            MaterialCardView card = new MaterialCardView(this);
            card.setRadius(dpToPx(8));
            card.setCardElevation(dpToPx(1));
            card.setStrokeWidth(dpToPx(1));
            card.setStrokeColor(0xFFE0E0E0);
            card.setCardBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = dpToPx(8);
            card.setLayoutParams(cardLp);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));

            TextView tvText = new TextView(this);
            tvText.setText(label + ") " + (opt.getContent() != null ? opt.getContent() : ""));
            tvText.setTextSize(13);
            tvText.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvText.setLayoutParams(textLp);
            row.addView(tvText);

            TextView btnDung = new TextView(this);
            btnDung.setText("Đúng");
            btnDung.setTextSize(12);
            btnDung.setGravity(Gravity.CENTER);
            btnDung.setTypeface(null, android.graphics.Typeface.BOLD);
            boolean isDung = Boolean.TRUE.equals(currentVal);
            btnDung.setTextColor(isDung ? Color.WHITE : 0xFF2E7D32);
            android.graphics.drawable.GradientDrawable gdDung = new android.graphics.drawable.GradientDrawable();
            gdDung.setColor(isDung ? 0xFF2E7D32 : 0xFFE8F5E9);
            gdDung.setCornerRadius(dpToPx(6));
            btnDung.setBackground(gdDung);
            btnDung.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
            LinearLayout.LayoutParams dungLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            dungLp.rightMargin = dpToPx(6);
            btnDung.setLayoutParams(dungLp);
            btnDung.setOnClickListener(v -> {
                currentTf.put(propKey, true);
                selectedAnswers.put(questionId, opt.getId());
                submitAnswer(questionId, opt.getId());
                displayBackendQuestion(currentBackendQuestion);
            });
            row.addView(btnDung);

            TextView btnSai = new TextView(this);
            btnSai.setText("Sai");
            btnSai.setTextSize(12);
            btnSai.setGravity(Gravity.CENTER);
            btnSai.setTypeface(null, android.graphics.Typeface.BOLD);
            boolean isSai = Boolean.FALSE.equals(currentVal);
            btnSai.setTextColor(isSai ? Color.WHITE : 0xFFC62828);
            android.graphics.drawable.GradientDrawable gdSai = new android.graphics.drawable.GradientDrawable();
            gdSai.setColor(isSai ? 0xFFC62828 : 0xFFFFEBEE);
            gdSai.setCornerRadius(dpToPx(6));
            btnSai.setBackground(gdSai);
            btnSai.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
            btnSai.setOnClickListener(v -> {
                currentTf.put(propKey, false);
                selectedAnswers.put(questionId, opt.getId());
                submitAnswer(questionId, opt.getId());
                displayBackendQuestion(currentBackendQuestion);
            });
            row.addView(btnSai);

            card.addView(row);
            binding.llOptions.addView(card);
        }
    }

    private void renderMatchingWidget(Long questionId, List<Question.Option> opts) {
        binding.llOptions.addView(createFormatBadge(
                "PHẦN 3: GHÉP HỢP (NỐI CẶP)", 0xFFE65100, 0xFFFFF3E0));

        Map<String, String> currentMatch = matchingAnswers.computeIfAbsent(questionId, k -> new HashMap<>());
        if (opts == null || opts.isEmpty()) return;

        for (int i = 0; i < opts.size(); i++) {
            Question.Option opt = opts.get(i);
            String itemKey = String.valueOf(i + 1);
            String selectedChoice = currentMatch.get(itemKey);

            MaterialCardView card = new MaterialCardView(this);
            card.setRadius(dpToPx(8));
            card.setCardElevation(dpToPx(1));
            card.setStrokeWidth(dpToPx(1));
            card.setStrokeColor(0xFFE0E0E0);
            card.setCardBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = dpToPx(8);
            card.setLayoutParams(cardLp);

            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));

            TextView tvItem = new TextView(this);
            tvItem.setText((i + 1) + ". " + (opt.getOptionText() != null ? opt.getOptionText() : ""));
            tvItem.setTextSize(13);
            tvItem.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            container.addView(tvItem);

            LinearLayout chipsRow = new LinearLayout(this);
            chipsRow.setOrientation(LinearLayout.HORIZONTAL);
            chipsRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams chipsLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            chipsLp.topMargin = dpToPx(6);
            chipsRow.setLayoutParams(chipsLp);

            TextView tvPrompt = new TextView(this);
            tvPrompt.setText("Ghép với: ");
            tvPrompt.setTextSize(11);
            tvPrompt.setTextColor(0xFF757575);
            chipsRow.addView(tvPrompt);

            String[] choices = {"A", "B", "C", "D"};
            Long optId = opt.getId();
            for (String ch : choices) {
                boolean isChosen = ch.equalsIgnoreCase(selectedChoice);
                TextView btnChip = new TextView(this);
                btnChip.setText(ch);
                btnChip.setTextSize(12);
                btnChip.setGravity(Gravity.CENTER);
                btnChip.setTypeface(null, android.graphics.Typeface.BOLD);
                btnChip.setTextColor(isChosen ? Color.WHITE : 0xFF1565C0);
                android.graphics.drawable.GradientDrawable gdChip = new android.graphics.drawable.GradientDrawable();
                gdChip.setColor(isChosen ? 0xFF1565C0 : 0xFFE3F2FD);
                gdChip.setCornerRadius(dpToPx(6));
                btnChip.setBackground(gdChip);
                btnChip.setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4));

                LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                chipLp.rightMargin = dpToPx(8);
                btnChip.setLayoutParams(chipLp);

                btnChip.setOnClickListener(v -> {
                    currentMatch.put(itemKey, ch);
                    selectedAnswers.put(questionId, optId != null ? optId : (questionId * 10 + 1));
                    submitAnswer(questionId, selectedAnswers.get(questionId));
                    displayQuestion(currentQuestion);
                });
                chipsRow.addView(btnChip);
            }

            container.addView(chipsRow);
            card.addView(container);
            binding.llOptions.addView(card);
        }
    }

    private void renderBackendMatchingWidget(Long questionId, List<QuestionOptionContentResponse> opts) {
        binding.llOptions.addView(createFormatBadge(
                "PHẦN 3: GHÉP HỢP (NỐI CẶP)", 0xFFE65100, 0xFFFFF3E0));

        Map<String, String> currentMatch = matchingAnswers.computeIfAbsent(questionId, k -> new HashMap<>());
        if (opts == null || opts.isEmpty()) return;

        for (int i = 0; i < opts.size(); i++) {
            QuestionOptionContentResponse opt = opts.get(i);
            String itemKey = String.valueOf(i + 1);
            String selectedChoice = currentMatch.get(itemKey);

            MaterialCardView card = new MaterialCardView(this);
            card.setRadius(dpToPx(8));
            card.setCardElevation(dpToPx(1));
            card.setStrokeWidth(dpToPx(1));
            card.setStrokeColor(0xFFE0E0E0);
            card.setCardBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = dpToPx(8);
            card.setLayoutParams(cardLp);

            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));

            TextView tvItem = new TextView(this);
            tvItem.setText((i + 1) + ". " + (opt.getContent() != null ? opt.getContent() : ""));
            tvItem.setTextSize(13);
            tvItem.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            container.addView(tvItem);

            LinearLayout chipsRow = new LinearLayout(this);
            chipsRow.setOrientation(LinearLayout.HORIZONTAL);
            chipsRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams chipsLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            chipsLp.topMargin = dpToPx(6);
            chipsRow.setLayoutParams(chipsLp);

            TextView tvPrompt = new TextView(this);
            tvPrompt.setText("Ghép với: ");
            tvPrompt.setTextSize(11);
            tvPrompt.setTextColor(0xFF757575);
            chipsRow.addView(tvPrompt);

            String[] choices = {"A", "B", "C", "D"};
            for (String ch : choices) {
                boolean isChosen = ch.equalsIgnoreCase(selectedChoice);
                TextView btnChip = new TextView(this);
                btnChip.setText(ch);
                btnChip.setTextSize(12);
                btnChip.setGravity(Gravity.CENTER);
                btnChip.setTypeface(null, android.graphics.Typeface.BOLD);
                btnChip.setTextColor(isChosen ? Color.WHITE : 0xFF1565C0);
                android.graphics.drawable.GradientDrawable gdChip = new android.graphics.drawable.GradientDrawable();
                gdChip.setColor(isChosen ? 0xFF1565C0 : 0xFFE3F2FD);
                gdChip.setCornerRadius(dpToPx(6));
                btnChip.setBackground(gdChip);
                btnChip.setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4));

                LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                chipLp.rightMargin = dpToPx(8);
                btnChip.setLayoutParams(chipLp);

                btnChip.setOnClickListener(v -> {
                    currentMatch.put(itemKey, ch);
                    selectedAnswers.put(questionId, opt.getId());
                    submitAnswer(questionId, opt.getId());
                    displayBackendQuestion(currentBackendQuestion);
                });
                chipsRow.addView(btnChip);
            }

            container.addView(chipsRow);
            card.addView(container);
            binding.llOptions.addView(card);
        }
    }

    private void renderShortAnswerWidget(Long questionId) {
        binding.llOptions.addView(createFormatBadge(
                "PHẦN 4: ĐIỀN SỐ / TRẢ LỜI NGẮN", 0xFF1B5E20, 0xFFE8F5E9));

        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dpToPx(10));
        card.setCardElevation(dpToPx(1));
        card.setStrokeWidth(dpToPx(1));
        card.setStrokeColor(0xFFE0E0E0);
        card.setCardBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dpToPx(10);
        card.setLayoutParams(cardLp);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Nhập kết quả hoặc đáp án của bạn:");
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        container.addView(tvTitle);

        android.widget.EditText etAnswer = new android.widget.EditText(this);
        etAnswer.setHint("Ví dụ: 37 hoặc 7.44...");
        etAnswer.setTextSize(15);
        etAnswer.setSingleLine(true);
        etAnswer.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(0xFFF9F9FB);
        gd.setCornerRadius(dpToPx(8));
        gd.setStroke(dpToPx(1), 0xFFCCCCCC);
        etAnswer.setBackground(gd);
        etAnswer.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
        String currentVal = shortAnswers.get(questionId);
        if (currentVal != null) {
            etAnswer.setText(currentVal);
        }
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        etLp.topMargin = dpToPx(8);
        etLp.bottomMargin = dpToPx(12);
        etAnswer.setLayoutParams(etLp);
        container.addView(etAnswer);

        com.google.android.material.button.MaterialButton btnSave =
                new com.google.android.material.button.MaterialButton(this);
        btnSave.setText(currentVal != null && !currentVal.isEmpty() ? "Cập nhật đáp án" : "Lưu đáp án");
        btnSave.setCornerRadius(dpToPx(8));
        btnSave.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        btnSave.setTextColor(Color.WHITE);
        btnSave.setOnClickListener(v -> {
            String textVal = etAnswer.getText() != null ? etAnswer.getText().toString().trim() : "";
            if (!textVal.isEmpty()) {
                shortAnswers.put(questionId, textVal);
                selectedAnswers.put(questionId, questionId * 10 + 1);
                submitAnswer(questionId, selectedAnswers.get(questionId));
                Toast.makeText(this, "Đã lưu đáp án: " + textVal, Toast.LENGTH_SHORT).show();
            } else {
                shortAnswers.remove(questionId);
                selectedAnswers.remove(questionId);
            }
        });
        container.addView(btnSave);

        card.addView(container);
        binding.llOptions.addView(card);
    }

    private void renderSingleChoiceWidget(Long questionId, List<Question.Option> opts, Long alreadySelected) {
        binding.llOptions.addView(createFormatBadge(
                "PHẦN 2: TRẮC NGHIỆM 4 LỰA CHỌN", 0xFF283593, 0xFFE8EAF6));

        for (int i = 0; i < opts.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = dpToPx(10);
            row.setLayoutParams(rowLp);

            MaterialCardView card1 = buildOptionCard(opts.get(i), questionId, alreadySelected);
            LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp1.rightMargin = dpToPx(5);
            card1.setLayoutParams(lp1);
            row.addView(card1);

            if (i + 1 < opts.size()) {
                MaterialCardView card2 = buildOptionCard(opts.get(i + 1), questionId, alreadySelected);
                LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                lp2.leftMargin = dpToPx(5);
                card2.setLayoutParams(lp2);
                row.addView(card2);
            } else {
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

    private void renderBackendSingleChoiceWidget(Long questionId, List<QuestionOptionContentResponse> opts, Long alreadySelected) {
        binding.llOptions.addView(createFormatBadge(
                "PHẦN 2: TRẮC NGHIỆM 4 LỰA CHỌN", 0xFF283593, 0xFFE8EAF6));

        for (int i = 0; i < opts.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = dpToPx(10);
            row.setLayoutParams(rowLp);

            MaterialCardView card1 = buildBackendOptionCard(
                    opts.get(i), questionId, alreadySelected, i);
            LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp1.rightMargin = dpToPx(5);
            card1.setLayoutParams(lp1);
            row.addView(card1);

            if (i + 1 < opts.size()) {
                MaterialCardView card2 = buildBackendOptionCard(
                        opts.get(i + 1), questionId, alreadySelected, i + 1);
                LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                lp2.leftMargin = dpToPx(5);
                card2.setLayoutParams(lp2);
                row.addView(card2);
            } else {
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

    private void displayQuestion(Question question) {
        if (question == null) {
            binding.tvQuestionText.setText("(Khong the tai cau hoi)");
            binding.llOptions.removeAllViews();
            return;
        }
        String content = question.getQuestionText();
        if (content == null || content.trim().isEmpty()) {
            content = "(Cau hoi chua co noi dung)";
        }
            binding.tvQuestionText.setText("Câu " + (currentIndex + 1) + ": " + content);
        binding.llOptions.removeAllViews();

        if (question.getOptions() == null || question.getOptions().isEmpty()) return;

        List<Question.Option> opts = question.getOptions();
        Long alreadySelected = selectedAnswers.get(question.getId());
        String qType = resolveQuestionType(question, null);

        if ("TRUE_FALSE".equalsIgnoreCase(qType)) {
            renderTrueFalseWidget(question.getId(), opts);
        } else if ("MATCHING".equalsIgnoreCase(qType)) {
            renderMatchingWidget(question.getId(), opts);
        } else if ("SHORT_ANSWER".equalsIgnoreCase(qType) || "FILL_IN_BLANK".equalsIgnoreCase(qType)) {
            renderShortAnswerWidget(question.getId());
        } else {
            renderSingleChoiceWidget(question.getId(), opts, alreadySelected);
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

    private void displayBackendQuestion(SessionQuestionContentResponse question) {
        if (question == null) {
            binding.tvQuestionText.setText("(Khong the tai noi dung cau hoi tu backend)");
            binding.llOptions.removeAllViews();
            return;
        }
        String content = question.getContent();
        if (content == null || content.trim().isEmpty()) {
            content = "(Cau hoi chua co noi dung)";
        }
            binding.tvQuestionText.setText("Câu " + (currentIndex + 1) + ": " + content);
        binding.llOptions.removeAllViews();

        List<QuestionOptionContentResponse> opts = question.getOptions();
        Long questionId = question.getId();
        Long alreadySelected = selectedAnswers.get(questionId);
        String qType = resolveQuestionType(null, question);

        if ("TRUE_FALSE".equalsIgnoreCase(qType)) {
            renderBackendTrueFalseWidget(questionId, opts);
        } else if ("MATCHING".equalsIgnoreCase(qType)) {
            renderBackendMatchingWidget(questionId, opts);
        } else if ("SHORT_ANSWER".equalsIgnoreCase(qType) || "FILL_IN_BLANK".equalsIgnoreCase(qType)) {
            renderShortAnswerWidget(questionId);
        } else {
            renderBackendSingleChoiceWidget(questionId, opts, alreadySelected);
        }
    }

    private MaterialCardView buildBackendOptionCard(
            QuestionOptionContentResponse option,
            Long questionId,
            Long alreadySelected,
            int optionIndex
    ) {
        Long optionId = option.getId();
        boolean isSelected = optionId != null && optionId.equals(alreadySelected);

        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dpToPx(10));
        card.setCardElevation(isSelected ? dpToPx(3) : dpToPx(1));
        card.setStrokeWidth(dpToPx(isSelected ? 2 : 1));
        card.setStrokeColor(isSelected ? strokeSelected : 0xFFDDDDDD);
        card.setCardBackgroundColor(isSelected ? colorSelected : colorDefault);
        card.setTag(optionId);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setGravity(Gravity.CENTER_VERTICAL);
        inner.setPadding(dpToPx(12), dpToPx(14), dpToPx(12), dpToPx(14));

        TextView tvLabel = new TextView(this);
        tvLabel.setText(String.valueOf((char) ('A' + optionIndex)) + ".");
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

        TextView tvText = new TextView(this);
        tvText.setText(option.getContent());
        tvText.setTextSize(14);
        tvText.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvText.setLayoutParams(textLp);

        inner.addView(tvLabel);
        inner.addView(tvText);

        if (isSelected) {
            TextView tvCheck = new TextView(this);
            tvCheck.setText("OK");
            tvCheck.setTextSize(12);
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
            selectedAnswers.put(questionId, optionId);
            displayBackendQuestion(currentBackendQuestion);
            submitAnswer(questionId, optionId);
        });

        return card;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void submitAnswer(Long questionId, Long optionId) {
        if (clientSideProcessing && !backendExamContent) {
            return;
        }
        if (!hasRemoteSession || sessionId == null) {
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("questionId", questionId);
        body.put("selectedOptionId", optionId);
        body.put("isBookmarked", bookmarkedQuestions.contains(questionId));
        body.put("timeSpentSeconds", 0);

        examApi.submitAnswer(sessionId, body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (backendExamContent
                        && (!response.isSuccessful() || response.body() == null
                        || !response.body().isSuccess())) {
                    Toast.makeText(ExamSessionActivity.this,
                            "Khong luu duoc cau tra loi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                if (backendExamContent) {
                    Toast.makeText(ExamSessionActivity.this,
                            "Khong luu duoc cau tra loi", Toast.LENGTH_SHORT).show();
                }
            }
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

        if (backendExamContent && hasRemoteSession) {
            submitBackendExam();
            return;
        }

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

    private void submitBackendExam() {
        if (sessionId == null) {
            Toast.makeText(this, "Khong co backend session", Toast.LENGTH_SHORT).show();
            return;
        }

        examApi.submitSession(sessionId).enqueue(new Callback<ApiResponse<ExamSession>>() {
            @Override
            public void onResponse(Call<ApiResponse<ExamSession>> call, Response<ApiResponse<ExamSession>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    ExamSession result = response.body().getData();
                    persistBackendReviewSnapshot();
                    String answersJson = new Gson().toJson(selectedAnswers);
                    int resultTotal = result.getTotalQuestions() > 0 ? result.getTotalQuestions() : totalQuestions;
                    int resultTime = result.getTimeSpentSeconds() > 0
                            ? result.getTimeSpentSeconds()
                            : getElapsedSeconds();
                    final boolean[] historySaveFailed = {false};
                    ExamHistoryEntry historyEntry = new ExamHistoryEntry(
                            examId,
                            examTitle != null ? examTitle : result.getExamTitle(),
                            examSubject,
                            resultTotal,
                            result.getCorrectAnswers(),
                            result.getScorePercentage(),
                            resultTime,
                            answersJson);
                    ExamHistoryRepository.getInstance().saveEntry(ExamSessionActivity.this, historyEntry,
                            () -> historySaveFailed[0] = true);
                    GamificationRepository.getInstance().recordExamCompleted(
                            ExamSessionActivity.this,
                            result.getCorrectAnswers(),
                            resultTotal);

                    Intent intent = new Intent(ExamSessionActivity.this, ExamResultActivity.class);
                    intent.putExtra("session_id", result.getId());
                    intent.putExtra("score", result.getScorePercentage());
                    intent.putExtra("correct", result.getCorrectAnswers());
                    intent.putExtra("total", resultTotal);
                    intent.putExtra("time_spent", resultTime);
                    intent.putExtra("exam_id", examId);
                    intent.putExtra("exam_title", examTitle);
                    intent.putExtra("exam_subject", examSubject != null ? examSubject : "");
                    intent.putExtra("selected_answers_json", answersJson);
                    intent.putExtra("history_save_failed", historySaveFailed[0]);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(ExamSessionActivity.this,
                            "Loi nop bai backend. Vui long thu lai.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ExamSession>> call, Throwable t) {
                Toast.makeText(ExamSessionActivity.this,
                        "Loi nop bai backend. Vui long thu lai.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void persistBackendReviewSnapshot() {
        if (sessionId == null) return;

        List<SessionQuestionContentResponse> orderedQuestions = new ArrayList<>();
        for (Long questionId : questionIds) {
            SessionQuestionContentResponse question = backendQuestionCache.get(questionId);
            if (question != null) {
                orderedQuestions.add(question);
            }
        }

        SharedPreferences prefs = getSharedPreferences(BACKEND_REVIEW_PREFS, MODE_PRIVATE);
        prefs.edit()
                .putLong(perUserKey(PREF_SESSION_PREFIX, examId), sessionId)
                .putString(perUserKey(PREF_QUESTION_IDS_PREFIX, examId), new Gson().toJson(questionIds))
                .putString(perUserKey(PREF_QUESTIONS_PREFIX, examId), new Gson().toJson(orderedQuestions))
                .apply();
    }

    private void submitExamLocally() {
        int correct = 0;
        for (Map.Entry<Long, Long> entry : selectedAnswers.entrySet()) {
            Long qId = entry.getKey();
            Long selectedOptionId = entry.getValue();
            Question q = questionCache.get(qId);
            if (q != null) {
                String qType = resolveQuestionType(q, null);
                if ("SHORT_ANSWER".equalsIgnoreCase(qType) || "FILL_IN_BLANK".equalsIgnoreCase(qType)) {
                    String entered = shortAnswers.get(qId);
                    String expected = null;
                    if (q.getOptions() != null && !q.getOptions().isEmpty()) {
                        expected = q.getOptions().get(0).getOptionText();
                    }
                    if (entered != null && expected != null && entered.trim().equalsIgnoreCase(expected.trim())) {
                        correct++;
                    }
                } else if (q.getOptions() != null) {
                    for (Question.Option opt : q.getOptions()) {
                        if (opt.getId() != null && opt.getId().equals(selectedOptionId) && opt.isCorrect()) {
                            correct++;
                            break;
                        }
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
        if (hasRemoteSession && sessionId != null) {
            List<ClientSubmitRequest.AnswerItem> answerItems = new ArrayList<>();
            for (int i = 0; i < questionIds.size(); i++) {
                Long qId = questionIds.get(i);
                Long selected = selectedAnswers.get(qId);
                answerItems.add(new ClientSubmitRequest.AnswerItem(
                        qId,
                        selected,
                        i + 1,
                        bookmarkedQuestions.contains(qId)
                ));
            }
            ClientSubmitRequest resultBody = new ClientSubmitRequest(
                    score, correct, total, timeSpentSeconds, answerItems);
            examApi.submitClientResult(sessionId, resultBody).enqueue(new Callback<ApiResponse<ExamSession>>() {
                @Override public void onResponse(Call<ApiResponse<ExamSession>> call, Response<ApiResponse<ExamSession>> response) {}
                @Override public void onFailure(Call<ApiResponse<ExamSession>> call, Throwable t) {}
            });
        }

        // Serialize selectedAnswers để truyền sang ExamResultActivity → ExamReviewActivity
        String answersJson = new Gson().toJson(selectedAnswers);

        // Lưu lịch sử bài làm vào local storage (async, callback nếu lỗi lưu)
        ExamHistoryEntry historyEntry = new ExamHistoryEntry(
                examId, examTitle, examSubject,
                total, correct, score, timeSpentSeconds, answersJson);
        final boolean[] historySaveFailed = {false};
        ExamHistoryRepository.getInstance().saveEntry(this, historyEntry,
                () -> historySaveFailed[0] = true);
        GamificationRepository.getInstance().recordExamCompleted(this, correct, total);

        if (offlineDownloadedSession) {
            OfflineExamSyncRepository.getInstance().enqueueResult(
                    this,
                    OfflineExamSyncRepository.item(
                            examId,
                            score,
                            correct,
                            total,
                            timeSpentSeconds,
                            buildOfflineSyncAnswers()));
        }

        Intent intent = new Intent(ExamSessionActivity.this, ExamResultActivity.class);
        intent.putExtra("session_id", sessionId != null ? sessionId : System.currentTimeMillis());
        intent.putExtra("score", score);
        intent.putExtra("correct", correct);
        intent.putExtra("total", total);
        intent.putExtra("time_spent", timeSpentSeconds);
        intent.putExtra("exam_id", examId);
        intent.putExtra("exam_title", examTitle);
        intent.putExtra("exam_subject", examSubject != null ? examSubject : "");
        intent.putExtra("selected_answers_json", answersJson);
        intent.putExtra("history_save_failed", historySaveFailed[0]);
        startActivity(intent);
        finish();
    }

    private List<OfflineExamSyncRepository.Answer> buildOfflineSyncAnswers() {
        List<OfflineExamSyncRepository.Answer> answers = new ArrayList<>();
        for (int i = 0; i < questionIds.size(); i++) {
            Long qId = questionIds.get(i);
            answers.add(OfflineExamSyncRepository.answer(
                    qId,
                    selectedAnswers.get(qId),
                    i + 1,
                    bookmarkedQuestions.contains(qId)));
        }
        return answers;
    }

    private int getElapsedSeconds() {
        if (sessionStartMillis <= 0) return 0;
        int elapsed = (int) ((System.currentTimeMillis() - sessionStartMillis) / 1000);
        return Math.max(elapsed, 0);
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
        Long qId = currentBackendQuestion != null && backendExamContent
                ? currentBackendQuestion.getId()
                : currentQuestion != null ? currentQuestion.getId() : null;
        if (qId == null) return;
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
        Long qId = currentBackendQuestion != null && backendExamContent
                ? currentBackendQuestion.getId()
                : currentQuestion != null ? currentQuestion.getId() : null;
        if (qId != null && bookmarkedQuestions.contains(qId)) {
            binding.btnBookmark.setColorFilter(
                    ContextCompat.getColor(this, R.color.warning));
        } else {
            binding.btnBookmark.setColorFilter(
                    ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void showQuestionGrid() {
        if (questionIds == null || questionIds.isEmpty() || totalQuestions <= 0) {
            showEmptyExamAndExit("Đề thi này hiện chưa có câu hỏi nào.");
            return;
        }
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
    @SuppressWarnings("MissingSuperCall")
    // super.onBackPressed() intentionally not called — back press shows exam exit dialog
    public void onBackPressed() {
        confirmExit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }
}
