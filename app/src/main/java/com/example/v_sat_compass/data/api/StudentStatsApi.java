package com.example.v_sat_compass.data.api;

import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.TopicStatsResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface StudentStatsApi {

    @GET("my-stats/topics")
    Call<ApiResponse<List<TopicStatsResponse>>> getTopicStats();

    @GET("my-stats/weak-topics")
    Call<ApiResponse<List<TopicStatsResponse>>> getWeakTopicStats();
}
