package com.example.v_sat_compass.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.v_sat_compass.VsatApp;

/**
 * Helper class để kiểm tra và quản lý quyền người dùng.
 *
 * 4 role:
 *   STUDENT         - Học viên (mặc định)
 *   COLLABORATOR    - Cộng tác viên: tạo câu hỏi, gửi duyệt
 *   CONTENT_ADMIN   - Admin nội dung: duyệt câu hỏi, tạo đề, quản lý ticket
 *   SUPER_ADMIN     - Toàn quyền: quản lý user & role, audit log
 */
public class UserRoleHelper {

    public static final String ROLE_STUDENT       = "STUDENT";
    public static final String ROLE_COLLABORATOR  = "COLLABORATOR";
    public static final String ROLE_CONTENT_ADMIN = "CONTENT_ADMIN";
    public static final String ROLE_SUPER_ADMIN   = "SUPER_ADMIN";

    private static final String PREF_NAME  = "vsat_prefs";
    private static final String KEY_ROLE   = "user_role";
    private static final String KEY_NAME   = "user_name";
    private static final String KEY_EMAIL  = "user_email";
    private static final String KEY_USER_ID = "user_id";

    private static SharedPreferences prefs() {
        return VsatApp.getInstance().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ─── Save ────────────────────────────────────────────────────────────────

    public static void saveUserInfo(Long userId, String fullName, String email, String role) {
        prefs().edit()
                .putLong(KEY_USER_ID, userId != null ? userId : 0)
                .putString(KEY_NAME,  fullName != null ? fullName : "")
                .putString(KEY_EMAIL, email != null ? email : "")
                .putString(KEY_ROLE,  role != null ? role : ROLE_STUDENT)
                .apply();
    }

    // ─── Get ─────────────────────────────────────────────────────────────────

    public static String getRole() {
        return prefs().getString(KEY_ROLE, ROLE_STUDENT);
    }

    public static String getFullName() {
        return prefs().getString(KEY_NAME, "");
    }

    public static String getEmail() {
        return prefs().getString(KEY_EMAIL, "");
    }

    public static long getUserId() {
        return prefs().getLong(KEY_USER_ID, 0);
    }

    // ─── Role checks ──────────────────────────────────────────────────────────

    public static boolean isStudent() {
        return ROLE_STUDENT.equals(getRole());
    }

    public static boolean isCollaborator() {
        return ROLE_COLLABORATOR.equals(getRole());
    }

    public static boolean isContentAdmin() {
        return ROLE_CONTENT_ADMIN.equals(getRole());
    }

    public static boolean isSuperAdmin() {
        return ROLE_SUPER_ADMIN.equals(getRole());
    }

    /** Có quyền vào chế độ quản trị (CTV trở lên) */
    public static boolean canAccessAdminMode() {
        String role = getRole();
        return ROLE_COLLABORATOR.equals(role)
                || ROLE_CONTENT_ADMIN.equals(role)
                || ROLE_SUPER_ADMIN.equals(role);
    }

    /** Có quyền duyệt câu hỏi và tạo đề (Content Admin trở lên) */
    public static boolean canReviewAndCreateExam() {
        String role = getRole();
        return ROLE_CONTENT_ADMIN.equals(role) || ROLE_SUPER_ADMIN.equals(role);
    }

    /** Có quyền quản lý người dùng và gán role (Super Admin) */
    public static boolean canManageUsers() {
        return ROLE_SUPER_ADMIN.equals(getRole());
    }

    /** Nhãn hiển thị vai trò tiếng Việt */
    public static String getRoleDisplayName(String role) {
        if (role == null) return "Học viên";
        switch (role) {
            case ROLE_COLLABORATOR:  return "Cộng tác viên";
            case ROLE_CONTENT_ADMIN: return "Admin nội dung";
            case ROLE_SUPER_ADMIN:   return "Quản trị viên";
            default:                 return "Học viên";
        }
    }

    /** Màu badge cho từng role */
    public static int getRoleColor(String role) {
        if (role == null) return 0xFF27AE60;
        switch (role) {
            case ROLE_SUPER_ADMIN:   return 0xFFE74C3C; // đỏ
            case ROLE_CONTENT_ADMIN: return 0xFF4A3ABA; // tím
            case ROLE_COLLABORATOR:  return 0xFF2196F3; // xanh
            default:                 return 0xFF27AE60; // xanh lá
        }
    }
}
