package com.example.v_sat_compass.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.AuthApi;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.UserProfile;
import com.example.v_sat_compass.databinding.FragmentProfileBinding;
import com.example.v_sat_compass.ui.admin.AdminActivity;
import com.example.v_sat_compass.ui.auth.LoginActivity;
import com.example.v_sat_compass.util.UserRoleHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadProfile();
        setupAdminAccess();

        binding.btnLogout.setOnClickListener(v -> {
            ApiClient.clearTokens();
            startActivity(new Intent(requireActivity(), LoginActivity.class));
            requireActivity().finishAffinity();
        });
    }

    // ─── Load profile từ API ──────────────────────────────────────────────────

    private void loadProfile() {
        // Hiển thị dữ liệu đã lưu ngay lập tức (UX nhanh)
        String cachedName = UserRoleHelper.getFullName();
        String cachedEmail = UserRoleHelper.getEmail();
        String cachedRole = UserRoleHelper.getRole();

        if (!cachedName.isEmpty()) binding.tvFullName.setText(cachedName);
        if (!cachedEmail.isEmpty()) binding.tvEmail.setText(cachedEmail);
        binding.tvRole.setText(cachedRole);
        binding.tvRoleBadge.setText(UserRoleHelper.getRoleDisplayName(cachedRole));

        // Sau đó fetch mới từ API
        AuthApi api = ApiClient.getClient().create(AuthApi.class);
        api.getMe().enqueue(new Callback<ApiResponse<UserProfile>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserProfile>> call,
                                   Response<ApiResponse<UserProfile>> response) {
                if (binding == null) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    UserProfile user = response.body().getData();

                    binding.tvFullName.setText(user.getFullName() != null ? user.getFullName() : "N/A");
                    binding.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "N/A");
                    binding.tvPhone.setText(user.getPhone() != null ? user.getPhone() : "Chưa cập nhật");
                    binding.tvRole.setText(user.getRole() != null ? user.getRole() : "STUDENT");
                    binding.tvRoleBadge.setText(UserRoleHelper.getRoleDisplayName(user.getRole()));

                    // Cập nhật cache
                    UserRoleHelper.saveUserInfo(user.getId(), user.getFullName(),
                            user.getEmail(), user.getRole());

                    // Re-setup admin access sau khi có role mới nhất từ API
                    setupAdminAccess();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserProfile>> call, Throwable t) {
                // Giữ dữ liệu cached
            }
        });
    }

    // ─── Phân quyền Admin ─────────────────────────────────────────────────────

    private void setupAdminAccess() {
        if (binding == null) return;

        if (UserRoleHelper.canAccessAdminMode()) {
            // Hiện card Trung tâm Quản trị và toggle
            binding.cardAdminCenter.setVisibility(View.VISIBLE);
            binding.cardAdminModeToggle.setVisibility(View.VISIBLE);

            // Tap card → xác nhận chuyển sang Admin mode
            binding.cardAdminCenter.setOnClickListener(v -> confirmSwitchToAdminMode());

            // Toggle switch
            binding.switchAdminMode.setOnCheckedChangeListener(null); // reset listener
            binding.switchAdminMode.setChecked(false);
            binding.switchAdminMode.setOnCheckedChangeListener((btn, checked) -> {
                if (checked) {
                    // Tắt toggle ngay để tránh bounce, chờ user xác nhận
                    btn.setChecked(false);
                    confirmSwitchToAdminMode();
                }
            });
        } else {
            // STUDENT: ẩn hoàn toàn phần Admin
            binding.cardAdminCenter.setVisibility(View.GONE);
            binding.cardAdminModeToggle.setVisibility(View.GONE);
        }
    }

    private void confirmSwitchToAdminMode() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Chuyển sang Chế độ Quản trị?")
                .setMessage("Bạn sẽ truy cập vào các công cụ quản lý câu hỏi, đề thi và người dùng. "
                        + "Một số tính năng học tập sẽ tạm ẩn.")
                .setPositiveButton("Xác nhận", (dialog, which) -> switchToAdminMode())
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void switchToAdminMode() {
        Intent intent = new Intent(requireContext(), AdminActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
