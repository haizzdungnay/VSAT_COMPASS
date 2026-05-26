package com.example.v_sat_compass.data.repository;

import com.example.v_sat_compass.data.api.AdminApi;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.UserItem;
import com.example.v_sat_compass.data.model.admin.PageResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminUserRepository {

    private final AdminApi adminApi;

    public AdminUserRepository() {
        this(ApiClient.getClient().create(AdminApi.class));
    }

    public AdminUserRepository(AdminApi adminApi) {
        this.adminApi = adminApi;
    }

    public interface UsersCallback {
        void onSuccess(List<UserItem> users);
        void onError(String message);
    }

    public interface VoidCallback {
        void onSuccess();
        void onError(String message);
    }

    public void loadUsers(String roleFilter, UsersCallback callback) {
        adminApi.getUsers(roleFilter, null, null, 0, 50).enqueue(new Callback<ApiResponse<PageResponse<UserItem>>>() {
            @Override
            public void onResponse(
                    Call<ApiResponse<PageResponse<UserItem>>> call,
                    Response<ApiResponse<PageResponse<UserItem>>> response
            ) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    PageResponse<UserItem> page = response.body().getData();
                    List<UserItem> users = page != null && page.getContent() != null
                            ? page.getContent() : Collections.emptyList();
                    callback.onSuccess(users);
                } else {
                    callback.onError(extractError(response));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PageResponse<UserItem>>> call, Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Lỗi mạng");
            }
        });
    }

    public void updateRole(Long userId, String role, VoidCallback callback) {
        Map<String, String> body = new HashMap<>();
        body.put("role", role);
        adminApi.updateUserRole(userId, body).enqueue(wrapVoid(callback));
    }

    public void lockUser(Long userId, VoidCallback callback) {
        adminApi.lockUser(userId).enqueue(wrapVoid(callback));
    }

    public void unlockUser(Long userId, VoidCallback callback) {
        adminApi.unlockUser(userId).enqueue(wrapVoid(callback));
    }

    private Callback<ApiResponse<Void>> wrapVoid(VoidCallback callback) {
        return new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError(extractError(response));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Lỗi mạng");
            }
        };
    }

    private static String extractError(Response<?> response) {
        if (response.errorBody() != null) {
            try {
                return response.errorBody().string();
            } catch (IOException ignored) {
            }
        }
        return "HTTP " + response.code();
    }
}
