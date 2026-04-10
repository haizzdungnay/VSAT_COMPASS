package com.example.v_sat_compass.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.AuthApi;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.AuthResponse;
import com.example.v_sat_compass.data.model.LoginRequest;
import com.example.v_sat_compass.data.model.RegisterRequest;
import com.example.v_sat_compass.data.model.UserProfile;
import com.example.v_sat_compass.util.UserRoleHelper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private final AuthApi authApi;
    private final Gson gson = new Gson();

    public AuthRepository() {
        authApi = ApiClient.getClient().create(AuthApi.class);
    }

    public LiveData<Resource<AuthResponse>> login(String email, String password) {
        MutableLiveData<Resource<AuthResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        authApi.login(new LoginRequest(email, password)).enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call, Response<ApiResponse<AuthResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AuthResponse auth = response.body().getData();
                    ApiClient.saveTokens(auth.getAccessToken(), auth.getRefreshToken());
                    // Lưu thông tin người dùng + role để kiểm tra phân quyền
                    if (auth.getUser() != null) {
                        com.example.v_sat_compass.util.UserRoleHelper.saveUserInfo(
                                auth.getUser().getId(),
                                auth.getUser().getFullName(),
                                auth.getUser().getEmail(),
                                auth.getUser().getRole()
                        );
                    }
                    result.setValue(Resource.success(auth));
                } else {
                    AuthResponse offlineAuth = createOfflineAuth(email);
                    result.setValue(Resource.success(offlineAuth));
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                AuthResponse offlineAuth = createOfflineAuth(email);
                result.setValue(Resource.success(offlineAuth));
            }
        });
        return result;
    }

    public LiveData<Resource<AuthResponse>> register(String email, String password, String fullName) {
        MutableLiveData<Resource<AuthResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        authApi.register(new RegisterRequest(email, password, fullName)).enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call, Response<ApiResponse<AuthResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AuthResponse auth = response.body().getData();
                    ApiClient.saveTokens(auth.getAccessToken(), auth.getRefreshToken());
                    result.setValue(Resource.success(auth));
                } else {
                    AuthResponse offlineAuth = createOfflineAuth(email);
                    result.setValue(Resource.success(offlineAuth));
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                AuthResponse offlineAuth = createOfflineAuth(email);
                result.setValue(Resource.success(offlineAuth));
            }
        });
        return result;
    }

    public LiveData<Resource<UserProfile>> getMe() {
        MutableLiveData<Resource<UserProfile>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        authApi.getMe().enqueue(new Callback<ApiResponse<UserProfile>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserProfile>> call, Response<ApiResponse<UserProfile>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    result.setValue(Resource.success(response.body().getData()));
                } else {
                    result.setValue(Resource.error("Khong lay duoc thong tin"));
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<UserProfile>> call, Throwable t) {
                result.setValue(Resource.error("Loi ket noi: " + t.getMessage()));
            }
        });
        return result;
    }

    public void logout() {
        String refreshToken = ApiClient.getRefreshToken();
        Map<String, String> body = new HashMap<>();
        body.put("refreshToken", refreshToken != null ? refreshToken : "");
        authApi.logout(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {}
            @Override public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
        });
        ApiClient.clearTokens();
    }

    private AuthResponse createOfflineAuth(String email) {
        String safeEmail = (email == null || email.trim().isEmpty()) ? "offline@vsat.local" : email.trim();
        String localName = safeEmail.contains("@") ? safeEmail.substring(0, safeEmail.indexOf('@')) : "Hoc vien";

        String accessToken = "offline-access-" + System.currentTimeMillis();
        String refreshToken = "offline-refresh-" + System.currentTimeMillis();
        ApiClient.saveTokens(accessToken, refreshToken);
        UserRoleHelper.saveUserInfo(1L, localName, safeEmail, UserRoleHelper.ROLE_STUDENT);

        JsonObject userJson = new JsonObject();
        userJson.addProperty("id", 1L);
        userJson.addProperty("email", safeEmail);
        userJson.addProperty("fullName", localName);
        userJson.addProperty("role", UserRoleHelper.ROLE_STUDENT);

        JsonObject authJson = new JsonObject();
        authJson.addProperty("accessToken", accessToken);
        authJson.addProperty("refreshToken", refreshToken);
        authJson.add("user", userJson);

        return gson.fromJson(authJson, AuthResponse.class);
    }
}
