package com.example.v_sat_compass.data.api;

import com.example.v_sat_compass.data.model.AiExplainRequest;
import com.example.v_sat_compass.data.model.AiExplainResponse;
import com.example.v_sat_compass.data.model.ApiResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AiApi {
    @POST("ai/explain")
    Call<ApiResponse<AiExplainResponse>> explain(@Body AiExplainRequest request);
}
