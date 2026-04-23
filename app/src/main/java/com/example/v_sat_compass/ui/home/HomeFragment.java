package com.example.v_sat_compass.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.AuthApi;
import com.example.v_sat_compass.data.api.ExamApi;
import com.example.v_sat_compass.data.local.LocalExamDataSource;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.data.model.ExamHistoryEntry;
import com.example.v_sat_compass.data.model.UserProfile;
import com.example.v_sat_compass.data.repository.ExamHistoryRepository;
import com.example.v_sat_compass.databinding.FragmentHomeBinding;
import com.example.v_sat_compass.ui.exam.ExamDetailActivity;
import com.example.v_sat_compass.ui.history.ExamHistoryActivity;

import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private FragmentHomeBinding binding;
    private final boolean clientSideProcessing = ApiClient.isClientSideExamProcessingEnabled();

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

                binding.progressScore.setProgress(stats.avgScore);
                binding.tvScoreValue.setText(String.valueOf(stats.avgScore));
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
                ExamHistoryEntry last = recent.get(0);
                binding.tvPracticeTitle.setText(last.getExamTitle());
                int pct = last.getTotalQuestions() > 0
                        ? (int) (last.getCorrectCount() * 100.0 / last.getTotalQuestions()) : 0;
                binding.progressPractice.setProgress(pct);
                binding.tvPracticeProgress.setText(pct + "%");
                binding.cardContinuePractice.setOnClickListener(v -> {
                    if (getContext() == null) return;
                    Exam exam = LocalExamDataSource.getInstance()
                            .getExamDetail(requireContext(), last.getExamId());
                    if (exam != null) navigateToExamDetail(exam);
                });
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
        if (clientSideProcessing) {
            renderExams(LocalExamDataSource.getInstance().getPublishedExams(requireContext()));
            return;
        }

        ExamApi examApi = ApiClient.getClient().create(ExamApi.class);
        examApi.getPublishedExams(null).enqueue(new Callback<ApiResponse<List<Exam>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Exam>>> call,
                                   Response<ApiResponse<List<Exam>>> response) {
                if (binding == null) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    renderExams(response.body().getData());
                } else {
                    renderExams(LocalExamDataSource.getInstance()
                            .getPublishedExams(requireContext()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Exam>>> call, Throwable t) {
                if (binding == null) return;
                renderExams(LocalExamDataSource.getInstance().getPublishedExams(requireContext()));
            }
        });
    }

    private void renderExams(List<Exam> exams) {
        if (binding == null || exams == null || exams.isEmpty()) return;

        List<Exam> upcoming = exams.subList(0, Math.min(5, exams.size()));
        binding.rvUpcomingExams.setAdapter(
                new UpcomingExamAdapter(upcoming, this::navigateToExamDetail));

        List<Exam> suggestions = exams.subList(0, Math.min(4, exams.size()));
        binding.rvSuggestions.setAdapter(
                new SuggestionAdapter(suggestions, this::navigateToExamDetail));
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
