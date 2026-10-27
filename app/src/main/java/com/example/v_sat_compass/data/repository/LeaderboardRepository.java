package com.example.v_sat_compass.data.repository;

import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.LeaderboardApi;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.LeaderboardEntry;
import com.example.v_sat_compass.data.model.LeaderboardMe;
import com.example.v_sat_compass.data.model.LeaderboardOptInRequest;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LeaderboardRepository {

    private final LeaderboardApi api;

    public LeaderboardRepository() {
        this(ApiClient.getClient().create(LeaderboardApi.class));
    }

    public LeaderboardRepository(LeaderboardApi api) {
        this.api = api;
    }

    public interface EntriesCallback {
        void onSuccess(List<LeaderboardEntry> entries);
        void onError(String message);
    }

    public interface MeCallback {
        void onSuccess(LeaderboardMe me);
        void onError(String message);
    }

    public void getTop(int limit, EntriesCallback callback) {
        api.getTop(limit).enqueue(new Callback<ApiResponse<List<LeaderboardEntry>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<LeaderboardEntry>>> call,
                                   Response<ApiResponse<List<LeaderboardEntry>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<LeaderboardEntry> list = response.body().getData();
                    callback.onSuccess(list != null ? list : new ArrayList<>());
                    return;
                }
                callback.onError("Khong tai duoc bang xep hang");
            }

            @Override
            public void onFailure(Call<ApiResponse<List<LeaderboardEntry>>> call, Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Loi ket noi");
            }
        });
    }

    public void getMe(MeCallback callback) {
        api.getMe().enqueue(new Callback<ApiResponse<LeaderboardMe>>() {
            @Override
            public void onResponse(Call<ApiResponse<LeaderboardMe>> call,
                                   Response<ApiResponse<LeaderboardMe>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                    return;
                }
                callback.onError("Khong tai duoc trang thai leaderboard");
            }

            @Override
            public void onFailure(Call<ApiResponse<LeaderboardMe>> call, Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Loi ket noi");
            }
        });
    }

    public void setOptIn(boolean optIn, MeCallback callback) {
        api.setOptIn(new LeaderboardOptInRequest(optIn))
                .enqueue(new Callback<ApiResponse<LeaderboardMe>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<LeaderboardMe>> call,
                                           Response<ApiResponse<LeaderboardMe>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess() && response.body().getData() != null) {
                            callback.onSuccess(response.body().getData());
                            return;
                        }
                        callback.onError("Khong cap nhat duoc trang thai");
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<LeaderboardMe>> call, Throwable t) {
                        callback.onError(t.getMessage() != null ? t.getMessage() : "Loi ket noi");
                    }
                });
    }
}
