package com.example.v_sat_compass.data.api;

import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.SubjectResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Public subject endpoint — no admin role required.
 * GET /subjects returns all active subjects ordered by displayOrder.
 */
public interface SubjectApi {

    @GET("subjects")
    Call<ApiResponse<List<SubjectResponse>>> getSubjects();
}
