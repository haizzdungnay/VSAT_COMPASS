package com.example.v_sat_compass.data.api;

import com.example.v_sat_compass.data.model.AdminStats;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.data.model.QuestionItem;
import com.example.v_sat_compass.data.model.UserItem;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * API dành cho CONTENT_ADMIN và SUPER_ADMIN.
 * Base URL: /admin/
 */
public interface AdminApi {

    // ─── Dashboard ────────────────────────────────────────────────────────────

    @GET("admin/stats")
    Call<ApiResponse<AdminStats>> getDashboardStats();

    // ─── Ngân hàng câu hỏi ────────────────────────────────────────────────────

    @GET("admin/questions")
    Call<ApiResponse<List<QuestionItem>>> getQuestions(
            @Query("status")     String status,
            @Query("subject_id") Long subjectId,
            @Query("keyword")    String keyword,
            @Query("page")       int page,
            @Query("size")       int size
    );

    @GET("admin/questions/{id}")
    Call<ApiResponse<QuestionItem>> getQuestionDetail(@Path("id") Long id);

    @PATCH("admin/questions/{id}/approve")
    Call<ApiResponse<Void>> approveQuestion(@Path("id") Long id);

    @PATCH("admin/questions/{id}/reject")
    Call<ApiResponse<Void>> rejectQuestion(
            @Path("id") Long id,
            @Body Map<String, String> body // { "comment": "..." }
    );

    @PATCH("admin/questions/{id}/request-revision")
    Call<ApiResponse<Void>> requestRevision(
            @Path("id") Long id,
            @Body Map<String, String> body // { "comment": "..." }
    );

    // ─── Quản lý đề thi ───────────────────────────────────────────────────────

    @GET("admin/exams")
    Call<ApiResponse<List<Exam>>> getAdminExams(
            @Query("status")  String status,
            @Query("subject") String subject,
            @Query("page")    int page,
            @Query("size")    int size
    );

    @POST("admin/exams")
    Call<ApiResponse<Exam>> createExam(@Body Map<String, Object> body);

    @PUT("admin/exams/{id}")
    Call<ApiResponse<Exam>> updateExam(@Path("id") Long id, @Body Map<String, Object> body);

    @PATCH("admin/exams/{id}/publish")
    Call<ApiResponse<Void>> publishExam(@Path("id") Long id);

    // ─── Quản lý người dùng (SUPER_ADMIN) ───────────────────────────────────

    @GET("admin/users")
    Call<ApiResponse<List<UserItem>>> getUsers(
            @Query("role")    String role,
            @Query("status")  String status,
            @Query("keyword") String keyword,
            @Query("page")    int page,
            @Query("size")    int size
    );

    @PATCH("admin/users/{id}/role")
    Call<ApiResponse<Void>> updateUserRole(
            @Path("id") Long userId,
            @Body Map<String, String> body // { "role": "COLLABORATOR" }
    );

    @PATCH("admin/users/{id}/lock")
    Call<ApiResponse<Void>> lockUser(@Path("id") Long userId);

    @PATCH("admin/users/{id}/unlock")
    Call<ApiResponse<Void>> unlockUser(@Path("id") Long userId);
}
