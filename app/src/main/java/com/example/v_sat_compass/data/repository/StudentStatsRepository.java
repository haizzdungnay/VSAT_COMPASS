package com.example.v_sat_compass.data.repository;

import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.StudentStatsApi;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.TopicStatsResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import retrofit2.Response;

public class StudentStatsRepository {

    private final StudentStatsApi api;

    public StudentStatsRepository() {
        this(ApiClient.getClient().create(StudentStatsApi.class));
    }

    public StudentStatsRepository(StudentStatsApi api) {
        this.api = api;
    }

    public interface TopicStatsCallback {
        void onSuccess(List<TopicStatsResponse> topics);
        void onError(String message);
    }

    public void loadTopicStats(TopicStatsCallback callback) {
        api.getTopicStats().enqueue(new retrofit2.Callback<ApiResponse<List<TopicStatsResponse>>>() {
            @Override
            public void onResponse(
                    retrofit2.Call<ApiResponse<List<TopicStatsResponse>>> call,
                    Response<ApiResponse<List<TopicStatsResponse>>> response
            ) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<TopicStatsResponse> data = response.body().getData();
                    callback.onSuccess(data != null ? data : Collections.emptyList());
                } else {
                    callback.onError("Không tải được thống kê chủ đề");
                }
            }

            @Override
            public void onFailure(
                    retrofit2.Call<ApiResponse<List<TopicStatsResponse>>> call,
                    Throwable t
            ) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Lỗi mạng");
            }
        });
    }

    public List<TopicStatsResponse> loadTopicStatsSync() throws IOException {
        Response<ApiResponse<List<TopicStatsResponse>>> response = api.getTopicStats().execute();
        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
            List<TopicStatsResponse> data = response.body().getData();
            return data != null ? data : Collections.emptyList();
        }
        return Collections.emptyList();
    }
}
