package com.example.v_sat_compass.ui.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.v_sat_compass.BuildConfig;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.AuthApi;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.UserProfile;
import com.example.v_sat_compass.data.repository.ExamHistoryRepository;
import com.example.v_sat_compass.databinding.FragmentProfileBinding;
import com.example.v_sat_compass.ui.admin.AdminActivity;
import com.example.v_sat_compass.ui.auth.LoginActivity;
import com.example.v_sat_compass.util.UserRoleHelper;

import com.google.android.material.snackbar.Snackbar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private static final String PROFILE_PREFS = "profile_local_overrides";
    private static final String KEY_AVATAR_URI = "avatar_uri";

    private FragmentProfileBinding binding;
    private ActivityResultLauncher<Intent> avatarPicker;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        avatarPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() != android.app.Activity.RESULT_OK
                            || result.getData() == null
                            || result.getData().getData() == null) {
                        return;
                    }
                    Uri uri = result.getData().getData();
                    try {
                        requireContext().getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) {
                        // Temporary URI providers still work for the current app session.
                    }
                    saveAvatarUri(uri.toString());
                    applyAvatarUri(uri.toString());
                });
    }

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
        loadLocalAvatar();
        setupProfileActions();
        setupAdminAccess();
        setupDevMenu();

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
                    if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()
                            && getSavedAvatarUri().isEmpty()) {
                        applyAvatarUri(user.getAvatarUrl());
                    }

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

    private void setupProfileActions() {
        binding.itemAccountSettings.setOnClickListener(v -> showEditProfileDialog());
        binding.itemChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        binding.ivAvatar.setOnClickListener(v -> openAvatarPicker());
    }

    private void showEditProfileDialog() {
        if (getContext() == null || binding == null) return;
        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        form.setPadding(pad, dp(8), pad, 0);

        EditText nameInput = buildInput("Ho ten", binding.tvFullName.getText().toString(),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        EditText emailInput = buildInput("Email", binding.tvEmail.getText().toString(),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText phoneInput = buildInput("So dien thoai", binding.tvPhone.getText().toString(),
                InputType.TYPE_CLASS_PHONE);

        form.addView(nameInput);
        form.addView(emailInput);
        form.addView(phoneInput);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Cap nhat tai khoan")
                .setView(form)
                .setPositiveButton("Luu", null)
                .setNegativeButton("Huy", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();
            if (name.length() < 2) {
                nameInput.setError("Nhap ho ten tu 2 ky tu");
                return;
            }
            if (!email.contains("@") || email.length() > 255) {
                emailInput.setError("Email khong hop le");
                return;
            }
            submitProfileUpdate(name, email, phone, dialog);
        }));
        dialog.show();
    }

    private void submitProfileUpdate(String name, String email, String phone, AlertDialog dialog) {
        Map<String, String> body = new HashMap<>();
        body.put("fullName", name);
        body.put("email", email);
        body.put("phone", phone);

        ApiClient.getClient().create(AuthApi.class).updateProfile(body)
                .enqueue(new Callback<ApiResponse<UserProfile>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<UserProfile>> call,
                                           Response<ApiResponse<UserProfile>> response) {
                        if (binding == null) return;
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess() && response.body().getData() != null) {
                            UserProfile user = response.body().getData();
                            binding.tvFullName.setText(user.getFullName());
                            binding.tvEmail.setText(user.getEmail());
                            binding.tvPhone.setText(user.getPhone() != null && !user.getPhone().isEmpty()
                                    ? user.getPhone() : "Chua cap nhat");
                            UserRoleHelper.saveUserInfo(user.getId(), user.getFullName(),
                                    user.getEmail(), user.getRole());
                            dialog.dismiss();
                            Snackbar.make(binding.getRoot(), "Da cap nhat ho so", Snackbar.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "Khong cap nhat duoc ho so", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<UserProfile>> call, Throwable t) {
                        if (getContext() != null) {
                            Toast.makeText(requireContext(), "Loi ket noi khi cap nhat ho so", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void showChangePasswordDialog() {
        if (getContext() == null) return;
        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        form.setPadding(pad, dp(8), pad, 0);

        EditText oldInput = buildInput("Mat khau cu", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText newInput = buildInput("Mat khau moi", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText confirmInput = buildInput("Nhap lai mat khau moi", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(oldInput);
        form.addView(newInput);
        form.addView(confirmInput);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Doi mat khau")
                .setView(form)
                .setPositiveButton("Luu", null)
                .setNegativeButton("Huy", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String oldPassword = oldInput.getText().toString();
            String newPassword = newInput.getText().toString();
            String confirm = confirmInput.getText().toString();
            if (oldPassword.isEmpty()) {
                oldInput.setError("Nhap mat khau cu");
                return;
            }
            if (newPassword.length() < 8 || !newPassword.matches(".*[A-Za-z].*") || !newPassword.matches(".*\\d.*")) {
                newInput.setError("Mat khau toi thieu 8 ky tu, co chu va so");
                return;
            }
            if (!newPassword.equals(confirm)) {
                confirmInput.setError("Mat khau nhap lai khong khop");
                return;
            }
            submitPasswordChange(oldPassword, newPassword, dialog);
        }));
        dialog.show();
    }

    private void submitPasswordChange(String oldPassword, String newPassword, AlertDialog dialog) {
        Map<String, String> body = new HashMap<>();
        body.put("oldPassword", oldPassword);
        body.put("newPassword", newPassword);
        ApiClient.getClient().create(AuthApi.class).changePassword(body)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        if (binding == null) return;
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            dialog.dismiss();
                            Snackbar.make(binding.getRoot(), "Da doi mat khau", Snackbar.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "Khong doi duoc mat khau", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        if (getContext() != null) {
                            Toast.makeText(requireContext(), "Loi ket noi khi doi mat khau", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private EditText buildInput(String hint, String value, int inputType) {
        EditText input = new EditText(requireContext());
        input.setHint(hint);
        input.setText(value == null ? "" : value);
        input.setSingleLine(true);
        input.setInputType(inputType);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        input.setLayoutParams(lp);
        return input;
    }

    private void openAvatarPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        avatarPicker.launch(intent);
    }

    private void loadLocalAvatar() {
        String avatarUri = getSavedAvatarUri();
        if (!avatarUri.isEmpty()) applyAvatarUri(avatarUri);
    }

    private void applyAvatarUri(String avatarUri) {
        if (binding == null || avatarUri == null || avatarUri.isEmpty()) return;
        try {
            binding.ivAvatar.setImageURI(Uri.parse(avatarUri));
        } catch (Exception ignored) {
            binding.ivAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);
        }
    }

    private void saveAvatarUri(String avatarUri) {
        prefs().edit().putString(KEY_AVATAR_URI, avatarUri).apply();
    }

    private String getSavedAvatarUri() {
        return prefs().getString(KEY_AVATAR_URI, "");
    }

    private SharedPreferences prefs() {
        return requireContext().getSharedPreferences(PROFILE_PREFS, android.content.Context.MODE_PRIVATE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

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

    // ─── Dev menu (DEBUG build only) ─────────────────────────────────────────

    private void setupDevMenu() {
        if (!BuildConfig.DEBUG) return;
        // Long-press vào tvFullName mở dev dialog inject mock data
        binding.tvFullName.setOnLongClickListener(v -> {
            showDevMenu();
            return true;
        });
    }

    private void showDevMenu() {
        if (getContext() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("[DEV] Dev Tools")
                .setItems(new String[]{
                        "Inject 50 lịch sử mock",
                        "Xóa toàn bộ lịch sử"
                }, (dialog, which) -> {
                    if (which == 0) {
                        injectMockHistory();
                    } else {
                        clearAllHistory();
                    }
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void injectMockHistory() {
        ExamHistoryRepository.getInstance().injectMockEntries(requireContext(), 50, () -> {
            if (binding == null) return;
            Snackbar.make(binding.getRoot(),
                    "[DEV] Đã inject 50 entries mock vào lịch sử",
                    Snackbar.LENGTH_SHORT).show();
        });
    }

    private void clearAllHistory() {
        // Xóa bằng cách ghi file rỗng
        ExamHistoryRepository.getInstance().clearAll(requireContext(), () -> {
            if (binding == null) return;
            Snackbar.make(binding.getRoot(),
                    "[DEV] Đã xóa toàn bộ lịch sử",
                    Snackbar.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
