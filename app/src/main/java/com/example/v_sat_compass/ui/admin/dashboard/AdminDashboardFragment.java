package com.example.v_sat_compass.ui.admin.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.api.AdminApi;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.model.AdminStats;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.databinding.FragmentAdminDashboardBinding;
import com.example.v_sat_compass.ui.admin.exam.AdminCreateExamActivity;
import com.example.v_sat_compass.ui.admin.questions.AdminQuestionBankFragment;
import com.example.v_sat_compass.ui.collaborator.CollaboratorCreateQuestionActivity;
import com.example.v_sat_compass.util.NetworkUtils;
import com.example.v_sat_compass.util.OfflineDemoDataHelper;
import com.example.v_sat_compass.util.UserRoleHelper;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardFragment extends Fragment {

    private FragmentAdminDashboardBinding binding;
    private View[] chartBars;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chartBars = new View[]{
                binding.barDay0, binding.barDay1, binding.barDay2, binding.barDay3,
                binding.barDay4, binding.barDay5, binding.barDay6
        };

        setGreeting();
        showStatsUnavailable();
        loadStats();
        setupQuickActions();
    }

    private void setGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting = hour < 12 ? "Chào buổi sáng" : (hour < 18 ? "Chào buổi chiều" : "Chào buổi tối");
        String name = UserRoleHelper.getFullName();
        if (name.isEmpty()) name = "Admin";
        binding.tvGreeting.setText(greeting + ", " + name + "!");
    }

    private void showStatsUnavailable() {
        String dash = getString(R.string.admin_stats_unavailable);
        binding.tvPendingQuestions.setText(dash);
        binding.tvRevenue.setText(dash);
        binding.tvTickets.setText(dash);
    }

    private void loadStats() {
        if (getContext() != null && !NetworkUtils.isOnline(requireContext())) {
            bindStats(OfflineDemoDataHelper.getDemoAdminStats());
            Toast.makeText(requireContext(), R.string.offline_demo_mode, Toast.LENGTH_SHORT).show();
            return;
        }
        AdminApi api = ApiClient.getClient().create(AdminApi.class);
        api.getDashboardStats().enqueue(new Callback<ApiResponse<AdminStats>>() {
            @Override
            public void onResponse(Call<ApiResponse<AdminStats>> call,
                                   Response<ApiResponse<AdminStats>> response) {
                if (binding == null) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    bindStats(response.body().getData());
                } else {
                    showStatsUnavailable();
                    Toast.makeText(requireContext(),
                            getString(R.string.admin_stats_load_error),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AdminStats>> call, Throwable t) {
                if (binding == null) return;
                showStatsUnavailable();
                Toast.makeText(requireContext(),
                        getString(R.string.admin_stats_load_error),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindStats(AdminStats stats) {
        binding.tvPendingQuestions.setText(String.valueOf(stats.getPendingQuestions()));
        binding.tvRevenue.setText(formatRevenue(stats.getRevenueToday()));
        binding.tvTickets.setText(String.valueOf(stats.getErrorTickets()));
        bindChart(stats.getSessionsLast7Days());
    }

    private void bindChart(int[] sessionsLast7Days) {
        if (chartBars == null || sessionsLast7Days == null || sessionsLast7Days.length != 7) {
            binding.cardChart.setVisibility(View.GONE);
            return;
        }
        int max = 0;
        int sum = 0;
        for (int value : sessionsLast7Days) {
            max = Math.max(max, value);
            sum += value;
        }
        if (sum == 0) {
            binding.cardChart.setVisibility(View.GONE);
            return;
        }
        binding.cardChart.setVisibility(View.VISIBLE);
        int maxHeightPx = (int) (110 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < chartBars.length; i++) {
            View bar = chartBars[i];
            int height = max > 0
                    ? Math.max((int) (maxHeightPx * (sessionsLast7Days[i] / (float) max)), (int) (8 * getResources().getDisplayMetrics().density))
                    : (int) (8 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) bar.getLayoutParams();
            lp.height = height;
            bar.setLayoutParams(lp);
        }
    }

    private void setupQuickActions() {
        binding.cardCreateQuestion.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CollaboratorCreateQuestionActivity.class)));

        binding.cardCreateExam.setOnClickListener(v -> {
            if (UserRoleHelper.canReviewAndCreateExam()) {
                startActivity(new Intent(requireContext(), AdminCreateExamActivity.class));
            }
        });

        binding.cardReviewQuestions.setOnClickListener(v -> {
            if (UserRoleHelper.canReviewAndCreateExam()) {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.admin_nav_host_fragment,
                                AdminQuestionBankFragment.newInstance("PENDING_REVIEW"))
                        .addToBackStack(null)
                        .commit();
            }
        });

        if (UserRoleHelper.isCollaborator()) {
            binding.cardCreateExam.setVisibility(View.GONE);
            binding.cardReviewQuestions.setVisibility(View.GONE);
        }
    }

    private String formatRevenue(long amount) {
        if (amount >= 1_000_000_000) return (amount / 1_000_000_000) + "B";
        if (amount >= 1_000_000)     return (amount / 1_000_000) + "M";
        if (amount >= 1_000)         return (amount / 1_000) + "K";
        return String.valueOf(amount);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
