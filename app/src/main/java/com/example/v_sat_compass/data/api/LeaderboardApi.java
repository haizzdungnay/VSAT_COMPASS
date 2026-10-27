package com.example.v_sat_compass.data.api;

import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.LeaderboardEntry;
import com.example.v_sat_compass.data.model.LeaderboardMe;
import com.example.v_sat_compass.data.model.LeaderboardOptInRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Query;

public interface LeaderboardApi {

    @GET("leaderboard")
    Call<ApiResponse<List<LeaderboardEntry>>> getTop(@Query("limit") int limit);

    @GET("auth/me/leaderboard")
    Call<ApiResponse<LeaderboardMe>> getMe();

    @PATCH("auth/me/leaderboard")
    Call<ApiResponse<LeaderboardMe>> setOptIn(@Body LeaderboardOptInRequest request);
}
