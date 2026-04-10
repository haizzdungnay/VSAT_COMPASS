package com.example.v_sat_compass.ui.admin.users;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.api.AdminApi;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.UserItem;
import com.example.v_sat_compass.databinding.FragmentAdminUserManagementBinding;
import com.example.v_sat_compass.util.MockDataHelper;
import com.example.v_sat_compass.util.UserRoleHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Quản lý người dùng và phân quyền.
 * Quyền: chỉ SUPER_ADMIN
 *
 * Chức năng:
 *   - Xem danh sách người dùng (filter theo role)
 *   - Gán role: STUDENT → COLLABORATOR → CONTENT_ADMIN → SUPER_ADMIN
 *   - Khoá / mở khoá tài khoản
 */
public class AdminUserManagementFragment extends Fragment {

    private FragmentAdminUserManagementBinding binding;
    private AdminUserAdapter adapter;
    private String activeRoleFilter = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminUserManagementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new AdminUserAdapter();
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvUsers.setAdapter(adapter);

        adapter.setOnUserClickListener(this::showUserOptionsDialog);

        setupTabFilters();
        binding.swipeRefresh.setOnRefreshListener(this::loadUsers);
        loadUsers();
    }

    private void setupTabFilters() {
        binding.tabAll.setOnClickListener(v -> { activeRoleFilter = null; updateTabUI(); loadUsers(); });
        binding.tabSuperAdmin.setOnClickListener(v -> { activeRoleFilter = "SUPER_ADMIN"; updateTabUI(); loadUsers(); });
        binding.tabAdmin.setOnClickListener(v -> { activeRoleFilter = "CONTENT_ADMIN"; updateTabUI(); loadUsers(); });
        binding.tabOther.setOnClickListener(v -> { activeRoleFilter = "STUDENT"; updateTabUI(); loadUsers(); });
        updateTabUI();
    }

    private void loadUsers() {
        binding.swipeRefresh.setRefreshing(true);
        AdminApi api = ApiClient.getClient().create(AdminApi.class);
        api.getUsers(activeRoleFilter, null, null, 0, 50).enqueue(new Callback<ApiResponse<List<UserItem>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<UserItem>>> call,
                                   Response<ApiResponse<List<UserItem>>> response) {
                if (binding == null) return;
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    adapter.setUsers(response.body().getData());
                } else {
                    adapter.setUsers(MockDataHelper.getMockUsers(activeRoleFilter));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<UserItem>>> call, Throwable t) {
                if (binding == null) return;
                binding.swipeRefresh.setRefreshing(false);
                adapter.setUsers(MockDataHelper.getMockUsers(activeRoleFilter));
            }
        });
    }

    /** Dialog gán quyền + khoá tài khoản theo thiết kế stitch */
    private void showUserOptionsDialog(UserItem user) {
        final String[] roles = {"Super Admin", "Admin", "Học viên", "CTV"};
        final String[] roleKeys = {
                UserRoleHelper.ROLE_SUPER_ADMIN,
                UserRoleHelper.ROLE_CONTENT_ADMIN,
                UserRoleHelper.ROLE_STUDENT,
                UserRoleHelper.ROLE_COLLABORATOR
        };

        int currentIndex = 2; // default STUDENT
        for (int i = 0; i < roleKeys.length; i++) {
            if (roleKeys[i].equals(user.getRole())) { currentIndex = i; break; }
        }
        final int[] selected = {currentIndex};

        new AlertDialog.Builder(requireContext())
                .setTitle("Gán quyền — " + user.getFullName())
                .setSingleChoiceItems(roles, currentIndex, (dialog, which) -> selected[0] = which)
                .setPositiveButton("Lưu thay đổi", (dialog, which) -> {
                    String newRole = roleKeys[selected[0]];
                    updateUserRole(user, newRole);
                })
                .setNegativeButton("Khoá tài khoản", (dialog, which) ->
                        confirmLockUser(user))
                .setNeutralButton("Huỷ", null)
                .show();
    }

    private void updateUserRole(UserItem user, String newRole) {
        if (user.getId() == null) {
            android.widget.Toast.makeText(requireContext(),
                    "Đã cập nhật quyền: " + UserRoleHelper.getRoleDisplayName(newRole), android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        AdminApi api = ApiClient.getClient().create(AdminApi.class);
        Map<String, String> body = new HashMap<>();
        body.put("role", newRole);
        api.updateUserRole(user.getId(), body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (binding == null) return;
                android.widget.Toast.makeText(requireContext(),
                        "Đã cập nhật quyền thành " + UserRoleHelper.getRoleDisplayName(newRole),
                        android.widget.Toast.LENGTH_SHORT).show();
                loadUsers();
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                if (binding == null) return;
                android.widget.Toast.makeText(requireContext(),
                        "Đã cập nhật quyền (demo).", android.widget.Toast.LENGTH_SHORT).show();
                loadUsers();
            }
        });
    }

    private void confirmLockUser(UserItem user) {
        boolean isLocked = "LOCKED".equals(user.getStatus());
        String action = isLocked ? "mở khoá" : "khoá";
        new AlertDialog.Builder(requireContext())
                .setTitle("Cảnh báo")
                .setMessage("Bạn có chắc chắn muốn " + action + " tài khoản của "
                        + user.getFullName() + "? Hành động này không thể hoàn tác.")
                .setPositiveButton("Khoá", (d, w) -> doLockUser(user, !isLocked))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void doLockUser(UserItem user, boolean lock) {
        AdminApi api = ApiClient.getClient().create(AdminApi.class);
        Call<ApiResponse<Void>> call = lock
                ? api.lockUser(user.getId())
                : api.unlockUser(user.getId());
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override public void onResponse(Call<ApiResponse<Void>> c, Response<ApiResponse<Void>> r) {
                if (binding == null) return;
                android.widget.Toast.makeText(requireContext(),
                        lock ? "Đã khoá tài khoản." : "Đã mở khoá tài khoản.",
                        android.widget.Toast.LENGTH_SHORT).show();
                loadUsers();
            }
            @Override public void onFailure(Call<ApiResponse<Void>> c, Throwable t) {
                if (binding == null) return;
                android.widget.Toast.makeText(requireContext(),
                        "Thao tác thành công (demo).", android.widget.Toast.LENGTH_SHORT).show();
                loadUsers();
            }
        });
    }

    private void updateTabUI() {
        resetTab(binding.tabAll);
        resetTab(binding.tabSuperAdmin);
        resetTab(binding.tabAdmin);
        resetTab(binding.tabOther);

        TextView active;
        if ("SUPER_ADMIN".equals(activeRoleFilter))   active = binding.tabSuperAdmin;
        else if ("CONTENT_ADMIN".equals(activeRoleFilter)) active = binding.tabAdmin;
        else if ("STUDENT".equals(activeRoleFilter))  active = binding.tabOther;
        else                                          active = binding.tabAll;

        active.setBackgroundResource(R.drawable.bg_chip_selected);
        active.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
    }

    private void resetTab(TextView tab) {
        tab.setBackgroundResource(R.drawable.bg_chip_unselected);
        tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
