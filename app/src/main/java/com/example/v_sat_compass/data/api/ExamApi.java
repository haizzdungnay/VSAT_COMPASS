package com.example.v_sat_compass.data.api;

import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.data.model.ExamSession;
import com.example.v_sat_compass.data.model.Question;

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
    Call<ApiResponse<List<Exam>>> getPublishedExams(
            @Query("subjectId") Long subjectId
    );

    @GET("exams/{id}")
    Call<ApiResponse<Exam>> getExamDetail(@Path("id") Long examId);

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
            @Body Map<String, Object> body
    );

    @GET("sessions/{sessionId}")
    Call<ApiResponse<ExamSession>> getSessionResult(@Path("sessionId") Long sessionId);

    @GET("sessions/{sessionId}/questions/{questionId}")
    Call<ApiResponse<Question>> getSessionQuestion(
            @Path("sessionId") Long sessionId,
            @Path("questionId") Long questionId
    );
}
