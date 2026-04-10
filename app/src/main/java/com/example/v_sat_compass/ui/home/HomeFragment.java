package com.example.v_sat_compass.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.AuthApi;
import com.example.v_sat_compass.data.api.ExamApi;
import com.example.v_sat_compass.data.local.LocalExamDataSource;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.data.model.UserProfile;
import com.example.v_sat_compass.databinding.FragmentHomeBinding;
import com.example.v_sat_compass.ui.exam.ExamDetailActivity;
import com.example.v_sat_compass.ui.exam.adapter.ExamAdapter;
import com.example.v_sat_compass.util.UserRoleHelper;

import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerViews();
        loadUserProfile();
        loadExams();
    }

    private void setupRecyclerViews() {
        binding.rvSuggestions.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvUpcomingExams.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
    }

    private void loadUserProfile() {
        AuthApi api = ApiClient.getClient().create(AuthApi.class);
        api.getMe().enqueue(new Callback<ApiResponse<UserProfile>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserProfile>> call, Response<ApiResponse<UserProfile>> response) {
                if (binding == null) return;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    UserProfile user = response.body().getData();
                    String name = user.getFullName();
                    if (name == null || name.isEmpty()) name = "bạn";
                    // Format: "Chào buổi sáng, Tên!"
                    int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    String timeGreeting;
                    if (hour < 12) timeGreeting = "Chào buổi sáng";
                    else if (hour < 18) timeGreeting = "Chào buổi chiều";
                    else timeGreeting = "Chào buổi tối";
                    binding.tvGreeting.setText(timeGreeting + ", " + name + "!");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserProfile>> call, Throwable t) {
                if (binding == null) return;
                setupDefaultGreeting();
            }
        });
        // Set default greeting while loading
        setupDefaultGreeting();
    }

    private void setupDefaultGreeting() {
        if (binding == null) return;
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String timeGreeting;
        if (hour < 12) timeGreeting = "Chào buổi sáng, bạn!";
        else if (hour < 18) timeGreeting = "Chào buổi chiều, bạn!";
        else timeGreeting = "Chào buổi tối, bạn!";
        binding.tvGreeting.setText(timeGreeting);
    }

    private void loadExams() {
        ExamApi examApi = ApiClient.getClient().create(ExamApi.class);
        examApi.getPublishedExams(null).enqueue(new Callback<ApiResponse<List<Exam>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Exam>>> call, Response<ApiResponse<List<Exam>>> response) {
                if (binding == null) return;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    renderExams(response.body().getData());
                } else {
                    renderExams(LocalExamDataSource.getInstance().getPublishedExams(requireContext()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Exam>>> call, Throwable t) {
                renderExams(LocalExamDataSource.getInstance().getPublishedExams(requireContext()));
            }
        });
    }

    private void renderExams(List<Exam> exams) {
        if (binding == null || exams == null || exams.isEmpty()) return;

        binding.tvTotalExams.setText(String.valueOf(exams.size()));

        List<Exam> upcoming = exams.subList(0, Math.min(5, exams.size()));
        UpcomingExamAdapter upcomingAdapter = new UpcomingExamAdapter(upcoming, this::navigateToExamDetail);
        binding.rvUpcomingExams.setAdapter(upcomingAdapter);

        List<Exam> suggestions = exams.subList(0, Math.min(4, exams.size()));
        SuggestionAdapter sugAdapter = new SuggestionAdapter(suggestions, this::navigateToExamDetail);
        binding.rvSuggestions.setAdapter(sugAdapter);
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
