package com.example.v_sat_compass.ui.admin.users;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.UserItem;
import com.example.v_sat_compass.data.repository.AdminUserRepository;
import com.example.v_sat_compass.databinding.FragmentAdminUserManagementBinding;
import com.example.v_sat_compass.util.NetworkUtils;
import com.example.v_sat_compass.util.OfflineDemoDataHelper;
import com.example.v_sat_compass.util.UserRoleHelper;

public class AdminUserManagementFragment extends Fragment {

    private FragmentAdminUserManagementBinding binding;
    private AdminUserAdapter adapter;
    private AdminUserRepository userRepository;
    private String activeRoleFilter = null;
    private boolean demoMode = false;

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

        userRepository = new AdminUserRepository();
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
        if (getContext() != null && !NetworkUtils.isOnline(requireContext())) {
            showDemoUsers();
            return;
        }
        demoMode = false;
        binding.swipeRefresh.setRefreshing(true);
        userRepository.loadUsers(activeRoleFilter, new AdminUserRepository.UsersCallback() {
            @Override
            public void onSuccess(java.util.List<UserItem> users) {
                if (binding == null) return;
                binding.swipeRefresh.setRefreshing(false);
                adapter.setUsers(users);
                boolean empty = users == null || users.isEmpty();
                binding.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                binding.rvUsers.setVisibility(empty ? View.GONE : View.VISIBLE);
                if (empty) {
                    binding.tvEmpty.setText("Không có người dùng");
                }
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.swipeRefresh.setRefreshing(false);
                adapter.setUsers(java.util.Collections.emptyList());
                binding.rvUsers.setVisibility(View.GONE);
                binding.tvEmpty.setVisibility(View.VISIBLE);
                binding.tvEmpty.setText(message != null ? message : "Không tải được danh sách");
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDemoUsers() {
        demoMode = true;
        binding.swipeRefresh.setRefreshing(false);
        java.util.List<UserItem> users = OfflineDemoDataHelper.getDemoUsers(activeRoleFilter);
        adapter.setUsers(users);
        binding.rvUsers.setVisibility(View.VISIBLE);
        binding.tvEmpty.setVisibility(View.GONE);
        Toast.makeText(requireContext(), R.string.offline_demo_mode, Toast.LENGTH_SHORT).show();
    }

    private void showUserOptionsDialog(UserItem user) {
        final String[] roles = {"Super Admin", "Admin", "Học viên", "CTV"};
        final String[] roleKeys = {
                UserRoleHelper.ROLE_SUPER_ADMIN,
                UserRoleHelper.ROLE_CONTENT_ADMIN,
                UserRoleHelper.ROLE_STUDENT,
                UserRoleHelper.ROLE_COLLABORATOR
        };

        int currentIndex = 2;
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
        if (demoMode) {
            user.setRole(newRole);
            adapter.notifyDataSetChanged();
            Toast.makeText(requireContext(), R.string.offline_demo_action, Toast.LENGTH_SHORT).show();
            return;
        }
        if (user.getId() == null) {
            Toast.makeText(requireContext(), "ID người dùng không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        userRepository.updateRole(user.getId(), newRole, new AdminUserRepository.VoidCallback() {
            @Override
            public void onSuccess() {
                if (binding == null) return;
                Toast.makeText(requireContext(),
                        "Đã cập nhật quyền thành " + UserRoleHelper.getRoleDisplayName(newRole),
                        Toast.LENGTH_SHORT).show();
                loadUsers();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmLockUser(UserItem user) {
        boolean isLocked = "LOCKED".equals(user.getStatus());
        String action = isLocked ? "mở khoá" : "khoá";
        new AlertDialog.Builder(requireContext())
                .setTitle("Cảnh báo")
                .setMessage("Bạn có chắc chắn muốn " + action + " tài khoản của "
                        + user.getFullName() + "?")
                .setPositiveButton(isLocked ? "Mở khoá" : "Khoá", (d, w) -> doLockUser(user, !isLocked))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void doLockUser(UserItem user, boolean lock) {
        if (demoMode) {
            user.setStatus(lock ? "LOCKED" : "ACTIVE");
            adapter.notifyDataSetChanged();
            Toast.makeText(requireContext(), R.string.offline_demo_action, Toast.LENGTH_SHORT).show();
            return;
        }
        if (user.getId() == null) return;
        AdminUserRepository.VoidCallback callback = new AdminUserRepository.VoidCallback() {
            @Override
            public void onSuccess() {
                if (binding == null) return;
                Toast.makeText(requireContext(),
                        lock ? "Đã khoá tài khoản." : "Đã mở khoá tài khoản.",
                        Toast.LENGTH_SHORT).show();
                loadUsers();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        };
        if (lock) {
            userRepository.lockUser(user.getId(), callback);
        } else {
            userRepository.unlockUser(user.getId(), callback);
        }
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
