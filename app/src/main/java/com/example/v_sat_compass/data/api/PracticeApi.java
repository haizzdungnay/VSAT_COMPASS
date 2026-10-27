package com.example.v_sat_compass.data.api;

import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.PracticeGradeRequest;
import com.example.v_sat_compass.data.model.PracticeGradeResponse;
import com.example.v_sat_compass.data.model.session.SessionQuestionContentResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface PracticeApi {
    @GET("practice/questions")
    Call<ApiResponse<List<SessionQuestionContentResponse>>> getQuestions(
            @Query("subtopicId") Long subtopicId,
            @Query("topicId") Long topicId,
            @Query("difficulty") String difficulty,
            @Query("limit") int limit);

    @POST("practice/grade")
    Call<ApiResponse<PracticeGradeResponse>> grade(@Body PracticeGradeRequest request);
}
