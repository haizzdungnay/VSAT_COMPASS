package com.example.v_sat_compass.ui.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import com.example.v_sat_compass.databinding.FragmentHomeBinding;
import com.example.v_sat_compass.ui.exam.ExamDetailActivity;
import com.example.v_sat_compass.ui.history.ExamHistoryActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final String PROFILE_PREFS = "profile_local_overrides";
    private static final String KEY_AVATAR_URI = "avatar_uri";

    private FragmentHomeBinding binding;
    private final boolean clientSideProcessing = ApiClient.isClientSideExamProcessingEnabled();
    private final boolean backendExamContent = ApiClient.USE_BACKEND_EXAM_CONTENT;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String localAvatarUri = "";

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
        loadLocalAvatar();
        loadUserProfile();
        loadExams();
        loadHistoryStats();
        setupHistoryButton();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistoryStats();
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
            SharedPreferences prefs = appContext.getSharedPreferences(
                    PROFILE_PREFS, android.content.Context.MODE_PRIVATE);
            String avatarUri = prefs.getString(KEY_AVATAR_URI, "");
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
