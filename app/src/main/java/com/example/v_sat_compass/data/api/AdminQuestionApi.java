package com.example.v_sat_compass.data.api;

import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.admin.AdminReviewActionRequest;
import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.question.QuestionListItemResponse;
import com.example.v_sat_compass.data.model.question.QuestionResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AdminQuestionApi {

    @GET("admin/questions")
    Call<ApiResponse<PageResponse<QuestionListItemResponse>>> getReviewQueue(
            @Query("status") QuestionStatus status,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("admin/questions/{id}")
    Call<ApiResponse<QuestionResponse>> getQuestionDetail(@Path("id") Long id);

    @POST("admin/questions/{id}/approve")
    Call<ApiResponse<QuestionResponse>> approveQuestion(
            @Path("id") Long id,
            @Body AdminReviewActionRequest body
    );

    @POST("admin/questions/{id}/request-revision")
    Call<ApiResponse<QuestionResponse>> requestRevision(
            @Path("id") Long id,
            @Body AdminReviewActionRequest body
    );

    @POST("admin/questions/{id}/reject")
    Call<ApiResponse<QuestionResponse>> rejectQuestion(
            @Path("id") Long id,
            @Body AdminReviewActionRequest body
    );
}
