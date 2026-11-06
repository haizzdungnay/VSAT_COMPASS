package com.example.v_sat_compass.data.api;

import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.admission.AdmissionBenchmarkItem;
import com.example.v_sat_compass.data.model.admission.AdmissionPredictionResult;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AdmissionApi {

    @GET("admissions/benchmarks")
    Call<ApiResponse<List<AdmissionBenchmarkItem>>> getBenchmarks(
            @Query("subjectCombo") String subjectCombo,
            @Query("year") Integer year
    );

    @POST("admissions/predict")
    Call<ApiResponse<List<AdmissionPredictionResult>>> predict(
            @Body Map<String, Object> body
    );
}
