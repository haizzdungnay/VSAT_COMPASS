package com.example.v_sat_compass.data.api;

import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.ExamSession;
import com.example.v_sat_compass.data.model.PublicExamDetailResponse;
import com.example.v_sat_compass.data.model.PublicExamSummaryResponse;
import com.example.v_sat_compass.data.model.Question;
import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.data.model.session.SessionAnswerKeysResponse;
import com.example.v_sat_compass.data.model.session.SessionQuestionContentResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ExamApi {

    @GET("exams")
    Call<ApiResponse<PageResponse<PublicExamSummaryResponse>>> getPublishedExams(
            @Query("subjectId") Long subjectId,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("exams/{id}")
    Call<ApiResponse<PublicExamDetailResponse>> getExamDetail(@Path("id") Long examId);

    @POST("sessions/start")
    Call<ApiResponse<ExamSession>> startSession(@Body Map<String, Long> body);

    @POST("sessions/{sessionId}/answers")
    Call<ApiResponse<Void>> submitAnswer(
            @Path("sessionId") Long sessionId,
            @Body Map<String, Object> body
    );

    @POST("sessions/{sessionId}/submit")
    Call<ApiResponse<ExamSession>> submitSession(@Path("sessionId") Long sessionId);

    // Client-side processing: send only the final calculated result (no per-answer calls)
    @POST("sessions/{sessionId}/client-submit")
    Call<ApiResponse<ExamSession>> submitClientResult(
            @Path("sessionId") Long sessionId,
            @Body com.example.v_sat_compass.data.model.ClientSubmitRequest body
    );

    @GET("sessions/{sessionId}")
    Call<ApiResponse<ExamSession>> getSessionResult(@Path("sessionId") Long sessionId);

    @GET("sessions/{sessionId}/questions/{questionId}")
    Call<ApiResponse<Question>> getSessionQuestion(
            @Path("sessionId") Long sessionId,
            @Path("questionId") Long questionId
    );

    @GET("sessions/{sessionId}/questions/{questionId}")
    Call<ApiResponse<SessionQuestionContentResponse>> getSessionQuestionContent(
            @Path("sessionId") long sessionId,
            @Path("questionId") long questionId
    );

    @GET("sessions/{sessionId}/answer-keys")
    Call<ApiResponse<SessionAnswerKeysResponse>> getSessionAnswerKeys(
            @Path("sessionId") long sessionId
    );
}
