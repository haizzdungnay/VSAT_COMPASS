package com.example.v_sat_compass.data.api;

import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.question.CreateQuestionRequest;
import com.example.v_sat_compass.data.model.question.QuestionListItemResponse;
import com.example.v_sat_compass.data.model.question.QuestionResponse;
import com.example.v_sat_compass.data.model.question.UpdateQuestionRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CollaboratorQuestionApi {

    @POST("collaborator/questions")
    Call<ApiResponse<QuestionResponse>> create(@Body CreateQuestionRequest req);

    @GET("collaborator/questions")
    Call<ApiResponse<PageResponse<QuestionListItemResponse>>> list(
            @Query("status") QuestionStatus status,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("collaborator/questions/{id}")
    Call<ApiResponse<QuestionResponse>> getById(@Path("id") Long id);

    @PUT("collaborator/questions/{id}")
    Call<ApiResponse<QuestionResponse>> update(
            @Path("id") Long id,
            @Body UpdateQuestionRequest req
    );

    @POST("collaborator/questions/{id}/submit-for-review")
    Call<ApiResponse<QuestionResponse>> submitForReview(@Path("id") Long id);
}
