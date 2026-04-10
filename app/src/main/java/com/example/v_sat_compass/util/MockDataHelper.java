package com.example.v_sat_compass.util;

import com.example.v_sat_compass.data.model.QuestionItem;
import com.example.v_sat_compass.data.model.UserItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Dữ liệu mẫu dùng khi backend chưa có hoặc offline.
 * Giúp demo giao diện quản trị đầy đủ.
 */
public class MockDataHelper {

    // ─── Câu hỏi mẫu ──────────────────────────────────────────────────────────

    public static List<QuestionItem> getMockQuestions(String statusFilter) {
        List<QuestionItem> list = new ArrayList<>();

        list.add(makeQuestion(1L, "MATH-V5-021", "Toán", "MULTIPLE_CHOICE", "PENDING", "Nam NV", 320, 5));
        list.add(makeQuestion(2L, "ENG-V4-012",  "Tiếng Anh", "MULTIPLE_CHOICE", "PENDING", "Lan PK", 210, 3));
        list.add(makeQuestion(3L, "MATH-V5-023", "Toán", "SHORT_ANSWER", "PENDING", "Hùng PQ", 180, 0));
        list.add(makeQuestion(4L, "MATH-V3-015", "Toán", "MULTIPLE_CHOICE", "APPROVED", "Minh LH", 450, 2));
        list.add(makeQuestion(5L, "ENG-V5-030",  "Tiếng Anh", "MULTIPLE_CHOICE", "APPROVED", "Lan PK", 310, 1));
        list.add(makeQuestion(6L, "MATH-V4-032", "Toán", "MULTIPLE_CHOICE", "PUBLISHED", "Nam NV", 890, 0));
        list.add(makeQuestion(7L, "MATH-V5-033", "Toán", "MULTIPLE_CHOICE", "PUBLISHED", "Hùng PQ", 760, 0));
        list.add(makeQuestion(8L, "ENG-V3-011",  "Tiếng Anh", "MULTIPLE_CHOICE", "NEEDS_REVISION", "Lan PK", 120, 8));
        list.add(makeQuestion(9L, "MATH-V2-008", "Toán", "MULTIPLE_CHOICE", "NEEDS_REVISION", "Minh LH", 95, 12));
        list.add(makeQuestion(10L,"ENG-V5-044",  "Tiếng Anh", "MULTIPLE_CHOICE", "PENDING", "Duc VA", 60, 0));

        if (statusFilter == null || statusFilter.isEmpty()) return list;

        List<QuestionItem> filtered = new ArrayList<>();
        for (QuestionItem q : list) {
            if (statusFilter.equals(q.getStatus())) filtered.add(q);
        }
        return filtered;
    }

    private static QuestionItem makeQuestion(Long id, String code, String subject,
                                              String type, String status,
                                              String creator, int views, int flags) {
        // Dùng reflection-free approach — tạo trực tiếp qua JSON trick
        try {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("question_code", code);
            json.put("subject_name", subject);
            json.put("question_type", type);
            json.put("status", status);
            json.put("creator_name", creator);
            json.put("view_count", views);
            json.put("flag_count", flags);
            json.put("difficulty", "MEDIUM");
            json.put("content", "Câu hỏi mẫu " + code + ": Chọn đáp án đúng nhất.");

            com.google.gson.Gson gson = new com.google.gson.Gson();
            return gson.fromJson(json.toString(), QuestionItem.class);
        } catch (Exception e) {
            return new QuestionItem();
        }
    }

    // ─── User mẫu ─────────────────────────────────────────────────────────────

    public static List<UserItem> getMockUsers(String roleFilter) {
        List<UserItem> list = new ArrayList<>();
        list.add(makeUser(1L, "Nguyễn Văn A", "a@test.vn", "SUPER_ADMIN",  "ACTIVE"));
        list.add(makeUser(2L, "Trần Thị B",   "b@test.vn", "CONTENT_ADMIN","ACTIVE"));
        list.add(makeUser(3L, "Lê Văn C",     "c@test.vn", "STUDENT",      "ACTIVE"));
        list.add(makeUser(4L, "Phạm Thị D",   "d@test.vn", "COLLABORATOR", "ACTIVE"));
        list.add(makeUser(5L, "Hoàng Văn E",  "e@test.vn", "STUDENT",      "LOCKED"));
        list.add(makeUser(6L, "Đặng Thị F",   "f@test.vn", "STUDENT",      "ACTIVE"));

        if (roleFilter == null || roleFilter.isEmpty()) return list;
        List<UserItem> filtered = new ArrayList<>();
        for (UserItem u : list) {
            if (roleFilter.equals(u.getRole())) filtered.add(u);
        }
        return filtered;
    }

    private static UserItem makeUser(Long id, String name, String email, String role, String status) {
        try {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("full_name", name);
            json.put("email", email);
            json.put("role", role);
            json.put("status", status);
            com.google.gson.Gson gson = new com.google.gson.Gson();
            return gson.fromJson(json.toString(), UserItem.class);
        } catch (Exception e) {
            return new UserItem();
        }
    }
}
