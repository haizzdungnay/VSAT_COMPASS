package com.example.v_sat_compass.ui.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.AuthApi;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.data.model.UserProfile;
import com.example.v_sat_compass.data.repository.ExamRepository;
import com.example.v_sat_compass.util.NetworkUtils;
import com.example.v_sat_compass.data.model.ExamHistoryEntry;
import com.example.v_sat_compass.data.repository.ExamHistoryRepository;
import com.example.v_sat_compass.data.model.GamificationState;
import com.example.v_sat_compass.data.repository.FlashcardRepository;
import com.example.v_sat_compass.data.repository.GamificationRepository;
import com.example.v_sat_compass.databinding.FragmentHomeBinding;
import com.example.v_sat_compass.ui.exam.ExamDetailActivity;
import com.example.v_sat_compass.ui.flashcards.FlashcardReviewActivity;
import com.example.v_sat_compass.ui.history.ExamHistoryActivity;
import com.example.v_sat_compass.ui.progress.ProgressActivity;
import com.example.v_sat_compass.util.AvatarPreferences;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private final boolean clientSideProcessing = ApiClient.isClientSideExamProcessingEnabled();
    private final boolean backendExamContent = ApiClient.USE_BACKEND_EXAM_CONTENT;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String localAvatarUri = "";
    private TextView tvStreakValue;
    private TextView tvLevelValue;
    private TextView tvXpValue;
    private TextView tvChallengeValue;
    private TextView tvFlashcardDueValue;
    private ProgressBar progressChallenge;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerViews();
        setupGamificationCard();
        setupFeatureCards();
        loadLocalAvatar();
        loadUserProfile();
        loadExams();
        loadHistoryStats();
        loadGamification();
        loadFlashcardDueCount();
        setupHistoryButton();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistoryStats();
        loadGamification();
        loadFlashcardDueCount();
    }

    private void setupGamificationCard() {
        if (binding == null || tvStreakValue != null) return;
        LinearLayout root = (LinearLayout) binding.getRoot().getChildAt(0);
        MaterialCardView card = new MaterialCardView(requireContext());
        card.setRadius(dp(12));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(getResources().getColor(R.color.white, null));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(dp(16), 0, dp(16), dp(12));
        card.setLayoutParams(cardLp);

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(content);

        TextView title = text("Dong luc hom nay", 16, true, R.color.text_primary);
        content.addView(title);

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, dp(8));
        content.addView(row);

        tvStreakValue = metric(row, "Streak");
        tvLevelValue = metric(row, "Level");
        tvXpValue = metric(row, "XP");

        tvChallengeValue = text("Daily challenge: 0/10 cau", 13, false, R.color.text_secondary);
        content.addView(tvChallengeValue);
        progressChallenge = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
        progressChallenge.setMax(GamificationRepository.DAILY_CHALLENGE_TARGET);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(6));
        barLp.topMargin = dp(6);
        progressChallenge.setLayoutParams(barLp);
        content.addView(progressChallenge);

        root.addView(card, 1);
    }

    private void setupFeatureCards() {
        if (binding == null || tvFlashcardDueValue != null) return;
        LinearLayout root = (LinearLayout) binding.getRoot().getChildAt(0);
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(dp(16), 0, dp(16), dp(12));
        row.setLayoutParams(rowLp);

        MaterialCardView flashcards = shortcutCard("Flashcards", "Due today", true);
        flashcards.setOnClickListener(v -> startActivity(new Intent(requireContext(), FlashcardReviewActivity.class)));
        MaterialCardView progress = shortcutCard("Progress", "Score trend", false);
        progress.setOnClickListener(v -> startActivity(new Intent(requireContext(), ProgressActivity.class)));

        row.addView(flashcards);
        row.addView(progress);
        root.addView(row, 2);
    }

    private MaterialCardView shortcutCard(String titleText, String captionText, boolean flashcardCard) {
        MaterialCardView card = new MaterialCardView(requireContext());
        card.setRadius(dp(12));
        card.setCardElevation(dp(2));
        card.setClickable(true);
        card.setFocusable(true);
        card.setCardBackgroundColor(getResources().getColor(R.color.white, null));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        cardLp.setMargins(flashcardCard ? 0 : dp(6), 0, flashcardCard ? dp(6) : 0, 0);
        card.setLayoutParams(cardLp);

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.addView(content);

        TextView title = text(titleText, 15, true, R.color.text_primary);
        content.addView(title);

        TextView caption = text(captionText, 12, false, R.color.text_secondary);
        caption.setPadding(0, dp(2), 0, 0);
        content.addView(caption);

        TextView value = text(flashcardCard ? "0 cards" : "Open", 18, true,
                flashcardCard ? R.color.primary : R.color.info);
        value.setPadding(0, dp(8), 0, 0);
        content.addView(value);
        if (flashcardCard) tvFlashcardDueValue = value;
        return card;
    }

    private TextView metric(LinearLayout row, String label) {
        LinearLayout col = new LinearLayout(requireContext());
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(android.view.Gravity.CENTER);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView value = text("0", 20, true, R.color.primary);
        TextView caption = text(label, 11, false, R.color.text_secondary);
        col.addView(value);
        col.addView(caption);
        row.addView(col);
        return value;
    }

    private TextView text(String value, int sp, boolean bold, int color) {
        TextView tv = new TextView(requireContext());
        tv.setText(value);
        tv.setTextSize(sp);
        tv.setTextColor(getResources().getColor(color, null));
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    private void loadGamification() {
        if (getContext() == null) return;
        GamificationRepository.getInstance().getState(requireContext(), this::renderGamification);
    }

    private void renderGamification(GamificationState state) {
        if (binding == null || state == null || tvStreakValue == null) return;
        tvStreakValue.setText(String.valueOf(state.getCurrentStreak()));
        tvLevelValue.setText(String.valueOf(state.getLevel()));
        tvXpValue.setText(String.valueOf(state.getTotalXp()));
        int progress = Math.max(0, Math.min(GamificationRepository.DAILY_CHALLENGE_TARGET, state.getChallengeProgress()));
        tvChallengeValue.setText(state.isChallengeCompleted()
                ? "Daily challenge: da hoan thanh"
                : "Thử thách hằng ngày: " + progress + "/" + GamificationRepository.DAILY_CHALLENGE_TARGET + " câu");
        progressChallenge.setProgress(progress);
    }

    private void loadFlashcardDueCount() {
        if (getContext() == null) return;
        FlashcardRepository.getInstance().getDueCards(requireContext(), cards -> {
            if (binding == null || tvFlashcardDueValue == null) return;
            int count = cards != null ? cards.size() : 0;
            tvFlashcardDueValue.setText(count + (count == 1 ? " card" : " cards"));
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void setupRecyclerViews() {
        binding.rvSuggestions.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvUpcomingExams.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
    }

    private void loadUserProfile() {
        setupDefaultGreeting(); // placeholder trước khi API trả về
        AuthApi api = ApiClient.getClient().create(AuthApi.class);
        api.getMe().enqueue(new Callback<ApiResponse<UserProfile>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserProfile>> call,
                                   Response<ApiResponse<UserProfile>> response) {
                if (binding == null) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    UserProfile user = response.body().getData();
                    String name = user.getFullName();
                    if (name == null || name.isEmpty()) {
                        name = getString(R.string.home_greeting_default_name);
                    }
                    String timeGreeting = getTimeGreeting(false);
                    binding.tvGreeting.setText(
                            getString(R.string.home_greeting_format, timeGreeting, name));
                    if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()
                            && localAvatarUri.isEmpty()) {
                        applyAvatarUri(user.getAvatarUrl());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserProfile>> call, Throwable t) {
                Log.d(TAG, "loadUserProfile() API failed, using default greeting");
                // Greeting default đã được set từ setupDefaultGreeting()
            }
        });
    }

    private void setupDefaultGreeting() {
        if (binding == null) return;
        binding.tvGreeting.setText(getTimeGreeting(true));
    }

    /** @param withDefault true = trả về full greeting "Chào buổi sáng, bạn!", false = prefix only */
    private String getTimeGreeting(boolean withDefault) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (withDefault) {
            if (hour < 12) return getString(R.string.home_greeting_default_morning);
            if (hour < 18) return getString(R.string.home_greeting_default_afternoon);
            return getString(R.string.home_greeting_default_evening);
        } else {
            if (hour < 12) return getString(R.string.home_greeting_morning);
            if (hour < 18) return getString(R.string.home_greeting_afternoon);
            return getString(R.string.home_greeting_evening);
        }
    }

    private void loadHistoryStats() {
        if (getContext() == null) return;
        ExamHistoryRepository.getInstance().getStats(requireContext(), stats -> {
            if (binding == null) return;

            binding.tvTotalExams.setText(String.valueOf(stats.totalAttempts));
            
            // Set dynamic average score and progress bar value from repository data
            binding.progressScore.setProgress(stats.avgScore);
            binding.tvScoreValue.setText(String.valueOf(stats.avgScore));

            if (stats.totalAttempts > 0) {
                binding.tvAvgScore.setText(String.valueOf(stats.avgScore));

                long totalMins = stats.totalTimeSeconds / 60;
                if (totalMins < 60) {
                    binding.tvTotalTime.setText(
                            getString(R.string.home_stats_time_minutes, totalMins));
                } else {
                    binding.tvTotalTime.setText(
                            getString(R.string.home_stats_time_hours,
                                    totalMins / 60, totalMins % 60));
                }
            } else {
                binding.tvAvgScore.setText(getString(R.string.home_stats_score_empty));
                binding.tvTotalTime.setText(getString(R.string.home_stats_time_zero));
            }

            loadRecentForContinue();
        });
    }

    private void loadRecentForContinue() {
        if (getContext() == null || binding == null) return;
        ExamHistoryRepository.getInstance().getRecent(requireContext(), 1, recent -> {
            if (binding == null) return;
            if (!recent.isEmpty()) {
                binding.tvPracticeHeader.setVisibility(View.VISIBLE);
                binding.cardContinuePractice.setVisibility(View.VISIBLE);

                ExamHistoryEntry last = recent.get(0);
                binding.tvPracticeTitle.setText(last.getExamTitle());
                int pct = last.getTotalQuestions() > 0
                        ? (int) (last.getCorrectCount() * 100.0 / last.getTotalQuestions()) : 0;
                binding.progressPractice.setProgress(pct);
                binding.tvPracticeProgress.setText(pct + "%");
                
                View.OnClickListener clickListener = v -> openExamById(last.getExamId());
                binding.cardContinuePractice.setOnClickListener(clickListener);
                binding.btnContinuePractice.setOnClickListener(clickListener);
            } else {
                binding.tvPracticeHeader.setVisibility(View.GONE);
                binding.cardContinuePractice.setVisibility(View.GONE);
            }
        });
    }

    private void setupHistoryButton() {
        binding.tvViewAllHistory.setOnClickListener(v -> {
            if (getContext() == null) return;
            startActivity(new Intent(getContext(), ExamHistoryActivity.class));
        });
    }

    private void loadExams() {
        if (!backendExamContent && clientSideProcessing) {
            renderExams(ExamRepository.getInstance().getLocalPublishedExams(requireContext()));
            return;
        }

        if (getContext() != null && !NetworkUtils.isOnline(requireContext())) {
            Toast.makeText(requireContext(), R.string.exam_offline_fallback, Toast.LENGTH_SHORT).show();
            renderExams(ExamRepository.getInstance().getLocalPublishedExams(requireContext()));
            return;
        }

        ExamRepository.getInstance().loadPublishedExams(null, new ExamRepository.ExamsCallback() {
            @Override
            public void onSuccess(List<Exam> exams) {
                if (binding == null) return;
                renderExams(exams);
            }

            @Override
            public void onError(String message) {
                if (getContext() == null || binding == null) return;
                Toast.makeText(requireContext(),
                        message != null ? message : getString(R.string.exam_offline_fallback),
                        Toast.LENGTH_SHORT).show();
                renderExams(ExamRepository.getInstance().getLocalPublishedExams(requireContext()));
            }
        });
    }

    private void openExamById(long examId) {
        if (getContext() == null) return;
        if (!backendExamContent || !NetworkUtils.isOnline(requireContext())) {
            Exam exam = ExamRepository.getInstance().getLocalExamDetail(requireContext(), examId);
            if (exam != null) {
                navigateToExamDetail(exam);
            }
            return;
        }
        ExamRepository.getInstance().loadExamDetail(examId, new ExamRepository.ExamCallback() {
            @Override
            public void onSuccess(Exam exam) {
                if (getContext() == null) return;
                navigateToExamDetail(exam);
            }

            @Override
            public void onError(String message) {
                if (getContext() == null) return;
                Exam exam = ExamRepository.getInstance().getLocalExamDetail(requireContext(), examId);
                if (exam != null) {
                    navigateToExamDetail(exam);
                } else {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void renderExams(List<Exam> exams) {
        if (binding == null || exams == null || exams.isEmpty()) return;

        List<Exam> displayable = new ArrayList<>();
        for (Exam exam : exams) {
            if (exam == null || exam.getTotalQuestions() <= 0) continue;
            String title = exam.getTitle() != null ? exam.getTitle().toLowerCase() : "";
            String code = exam.getExamCode() != null ? exam.getExamCode().toLowerCase() : "";
            if (title.contains("smoke") || code.contains("smoke")) continue;
            displayable.add(exam);
        }
        if (displayable.isEmpty()) displayable = exams;

        List<Exam> upcoming = displayable.subList(0, Math.min(5, displayable.size()));
        binding.rvUpcomingExams.setAdapter(
                new UpcomingExamAdapter(upcoming, this::navigateToExamDetail));

        List<Exam> suggestions = displayable.subList(0, Math.min(4, displayable.size()));
        binding.rvSuggestions.setAdapter(
                new SuggestionAdapter(suggestions, this::navigateToExamDetail));
    }

    private void loadLocalAvatar() {
        if (getContext() == null) return;
        android.content.Context appContext = requireContext().getApplicationContext();
        new Thread(() -> {
            String avatarUri = AvatarPreferences.getCurrentUserAvatar(appContext);
            mainHandler.post(() -> {
                localAvatarUri = avatarUri != null ? avatarUri : "";
                if (!localAvatarUri.isEmpty()) applyAvatarUri(localAvatarUri);
            });
        }).start();
    }

    private void applyAvatarUri(String avatarUri) {
        if (binding == null || avatarUri == null || avatarUri.isEmpty()) return;
        try {
            binding.ivAvatar.setImageURI(Uri.parse(avatarUri));
        } catch (Exception ignored) {
            binding.ivAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);
        }
    }

    private void navigateToExamDetail(Exam exam) {
        if (getContext() == null) return;
        Intent intent = new Intent(getContext(), ExamDetailActivity.class);
        intent.putExtra("exam_id", exam.getId());
        intent.putExtra("exam_title", exam.getTitle());
        intent.putExtra("exam_description", exam.getDescription());
        intent.putExtra("exam_subject", exam.getSubjectName());
        intent.putExtra("total_questions", exam.getTotalQuestions());
        intent.putExtra("duration_minutes", exam.getDurationMinutes());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
