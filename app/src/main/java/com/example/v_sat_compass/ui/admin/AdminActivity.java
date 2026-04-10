package com.example.v_sat_compass.ui.admin;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.v_sat_compass.MainActivity;
import com.example.v_sat_compass.R;
import com.example.v_sat_compass.databinding.ActivityAdminBinding;
import com.example.v_sat_compass.util.UserRoleHelper;

/**
 * Activity chính cho chế độ Quản trị.
 * Chứa bottom navigation: Tổng quan | Ngân hàng | Đề thi | Người dùng
 *
 * Quyền truy cập:
 *   - COLLABORATOR   → Chỉ thấy tab Ngân hàng (câu hỏi của mình)
 *   - CONTENT_ADMIN  → Ngân hàng + Đề thi + Tổng quan
 *   - SUPER_ADMIN    → Toàn bộ 4 tab
 */
public class AdminActivity extends AppCompatActivity {

    private ActivityAdminBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();
        setupAdminBanner();
        applyRoleBasedAccess();
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.admin_nav_host_fragment);
        if (navHostFragment == null) return;

        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(binding.adminBottomNav, navController);
    }

    private void setupAdminBanner() {
        binding.tvSwitchToStudent.setOnClickListener(v -> showSwitchToStudentDialog());
    }

    private void applyRoleBasedAccess() {
        // COLLABORATOR: ẩn tab Tổng quan và Người dùng
        if (UserRoleHelper.isCollaborator()) {
            binding.adminBottomNav.getMenu().findItem(R.id.nav_admin_dashboard).setVisible(false);
            binding.adminBottomNav.getMenu().findItem(R.id.nav_admin_users).setVisible(false);
            // Chuyển start destination sang Ngân hàng câu hỏi
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.admin_nav_host_fragment);
            if (navHostFragment != null) {
                navHostFragment.getNavController()
                        .navigate(R.id.nav_admin_questions);
            }
        }

        // CONTENT_ADMIN: ẩn tab Người dùng (chỉ SUPER_ADMIN mới quản lý user)
        if (UserRoleHelper.isContentAdmin()) {
            binding.adminBottomNav.getMenu().findItem(R.id.nav_admin_users).setVisible(false);
        }
    }

    private void showSwitchToStudentDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Chuyển sang Chế độ Học viên?")
                .setMessage("Bạn sẽ rời khỏi khu vực quản trị và trở về giao diện học viên.")
                .setPositiveButton("Xác nhận", (dialog, which) -> switchToStudentMode())
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void switchToStudentMode() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Nếu đang ở tab đầu tiên thì hỏi xác nhận thoát
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.admin_nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.nav_admin_dashboard) {
                showSwitchToStudentDialog();
                return;
            }
        }
        super.onBackPressed();
    }
}
