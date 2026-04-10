package com.example.v_sat_compass.data.api;

import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.QuestionItem;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * API dành cho COLLABORATOR (Cộng tác viên).
 * Chỉ xem/sửa câu hỏi của chính mình.
 */
public interface CollaboratorApi {

    // ─── Không gian CTV ──────────────────────────────────────────────────────

    @GET("collaborator/questions")
    Call<ApiResponse<List<QuestionItem>>> getMyQuestions(
            @Query("status")  String status,
            @Query("keyword") String keyword,
            @Query("page")    int page,
            @Query("size")    int size
    );

    @GET("collaborator/questions/{id}")
    Call<ApiResponse<QuestionItem>> getMyQuestion(@Path("id") Long id);

    @POST("collaborator/questions")
    Call<ApiResponse<QuestionItem>> createQuestion(@Body Map<String, Object> body);

    @PUT("collaborator/questions/{id}")
    Call<ApiResponse<QuestionItem>> updateQuestion(
            @Path("id") Long id,
            @Body Map<String, Object> body
    );

    @DELETE("collaborator/questions/{id}")
    Call<ApiResponse<Void>> deleteQuestion(@Path("id") Long id);

    /** Gửi câu hỏi chờ duyệt */
    @POST("collaborator/questions/{id}/submit")
    Call<ApiResponse<Void>> submitQuestion(@Path("id") Long id);
}
