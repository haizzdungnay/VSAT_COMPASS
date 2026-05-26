package com.example.v_sat_compass.util;

import com.example.v_sat_compass.data.model.AdminStats;
import com.example.v_sat_compass.data.model.TopicStatsResponse;
import com.example.v_sat_compass.data.model.UserItem;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Dữ liệu demo khi thiết bị offline — không ghi đè dữ liệu server khi online.
 */
public final class OfflineDemoDataHelper {

    private static final Gson GSON = new Gson();

    private OfflineDemoDataHelper() {
    }

    public static List<UserItem> getDemoUsers(String roleFilter) {
        List<UserItem> list = new ArrayList<>();
        list.add(makeUser(1L, "Nguyễn Văn A", "a@test.vn", "SUPER_ADMIN", "ACTIVE"));
        list.add(makeUser(2L, "Trần Thị B", "b@test.vn", "CONTENT_ADMIN", "ACTIVE"));
        list.add(makeUser(3L, "Lê Văn C", "c@test.vn", "STUDENT", "ACTIVE"));
        list.add(makeUser(4L, "Phạm Thị D", "d@test.vn", "COLLABORATOR", "ACTIVE"));
        list.add(makeUser(5L, "Hoàng Văn E", "e@test.vn", "STUDENT", "LOCKED"));
        list.add(makeUser(6L, "Đặng Thị F", "f@test.vn", "STUDENT", "ACTIVE"));

        if (roleFilter == null || roleFilter.isEmpty()) {
            return list;
        }
        List<UserItem> filtered = new ArrayList<>();
        for (UserItem user : list) {
            if (roleFilter.equals(user.getRole())) {
                filtered.add(user);
            }
        }
        return filtered;
    }

    public static AdminStats getDemoAdminStats() {
        JsonObject json = new JsonObject();
        json.addProperty("pending_questions", 24);
        json.addProperty("revenue_today", 2_500_000L);
        json.addProperty("error_tickets", 5);
        json.add("sessions_last_7_days", GSON.toJsonTree(new int[]{4, 8, 12, 6, 15, 10, 9}));
        return GSON.fromJson(json, AdminStats.class);
    }

    public static List<TopicStatsResponse> getDemoTopicStats() {
        List<TopicStatsResponse> topics = new ArrayList<>();
        topics.add(topic("Hình học không gian", 45));
        topics.add(topic("Logarit & Hàm số mũ", 62));
        topics.add(topic("Số học & Đại số", 30));
        topics.add(topic("Giải tích", 55));
        topics.add(topic("Xác suất & Thống kê", 40));
        topics.add(topic("Vật lí hạt nhân", 25));
        return topics;
    }

    private static TopicStatsResponse topic(String name, int percentage) {
        TopicStatsResponse row = new TopicStatsResponse();
        row.setTopicName(name);
        row.setPercentage(percentage);
        row.setCorrect(percentage);
        row.setTotal(100);
        return row;
    }

    private static UserItem makeUser(Long id, String name, String email, String role, String status) {
        try {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("full_name", name);
            json.put("email", email);
            json.put("role", role);
            json.put("status", status);
            return GSON.fromJson(json.toString(), UserItem.class);
        } catch (Exception e) {
            return new UserItem();
        }
    }
}
