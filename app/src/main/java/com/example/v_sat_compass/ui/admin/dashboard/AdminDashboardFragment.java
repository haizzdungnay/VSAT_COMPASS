package com.example.v_sat_compass.ui.admin.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.v_sat_compass.data.api.AdminApi;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.model.AdminStats;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.databinding.FragmentAdminDashboardBinding;
import com.example.v_sat_compass.ui.admin.exam.AdminCreateExamActivity;
import com.example.v_sat_compass.ui.admin.questions.AdminQuestionBankFragment;
import com.example.v_sat_compass.ui.collaborator.QuestionEditorActivity;
import com.example.v_sat_compass.util.UserRoleHelper;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Màn hình Tổng quan Quản trị.
 * Hiển thị: câu hỏi chờ duyệt, doanh thu, ticket lỗi + hành động nhanh.
 * Quyền: CONTENT_ADMIN, SUPER_ADMIN
 */
public class AdminDashboardFragment extends Fragment {

    private FragmentAdminDashboardBinding binding;

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

        setGreeting();
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

    private void loadStats() {
        AdminApi api = ApiClient.getClient().create(AdminApi.class);
        api.getDashboardStats().enqueue(new Callback<ApiResponse<AdminStats>>() {
            @Override
            public void onResponse(Call<ApiResponse<AdminStats>> call,
                                   Response<ApiResponse<AdminStats>> response) {
                if (binding == null) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    AdminStats stats = response.body().getData();
                    binding.tvPendingQuestions.setText(String.valueOf(stats.getPendingQuestions()));
                    binding.tvRevenue.setText(formatRevenue(stats.getRevenueToday()));
                    binding.tvTickets.setText(String.valueOf(stats.getErrorTickets()));
                }
                // Nếu lỗi giữ giá trị placeholder (24, 2.5M, 5)
            }

            @Override
            public void onFailure(Call<ApiResponse<AdminStats>> call, Throwable t) {
                // Giữ giá trị placeholder khi offline
            }
        });
    }

    private void setupQuickActions() {
        // Tạo câu hỏi: CTV trở lên
        binding.cardCreateQuestion.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), QuestionEditorActivity.class));
        });

        // Tạo đề thi: chỉ CONTENT_ADMIN và SUPER_ADMIN
        binding.cardCreateExam.setOnClickListener(v -> {
            if (UserRoleHelper.canReviewAndCreateExam()) {
                startActivity(new Intent(requireContext(), AdminCreateExamActivity.class));
            }
        });

        // Duyệt câu hỏi: chỉ CONTENT_ADMIN và SUPER_ADMIN
        binding.cardReviewQuestions.setOnClickListener(v -> {
            if (UserRoleHelper.canReviewAndCreateExam()) {
                // Navigate đến tab Ngân hàng với filter PENDING
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(com.example.v_sat_compass.R.id.admin_nav_host_fragment,
                                AdminQuestionBankFragment.newInstance("PENDING"))
                        .addToBackStack(null)
                        .commit();
            }
        });

        // Ẩn nút tạo đề và duyệt nếu là CTV
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
