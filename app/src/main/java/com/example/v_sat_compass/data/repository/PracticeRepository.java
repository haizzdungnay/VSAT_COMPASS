package com.example.v_sat_compass.data.repository;

import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.PracticeApi;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.PracticeGradeRequest;
import com.example.v_sat_compass.data.model.PracticeGradeResponse;
import com.example.v_sat_compass.data.model.session.SessionQuestionContentResponse;

import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PracticeRepository {
    private final PracticeApi api;

    public PracticeRepository() {
        this(ApiClient.getClient().create(PracticeApi.class));
    }

    public PracticeRepository(PracticeApi api) {
        this.api = api;
    }

    public interface QuestionsCallback {
        void onSuccess(List<SessionQuestionContentResponse> questions);
        void onError(String message);
    }

    public interface GradeCallback {
        void onSuccess(PracticeGradeResponse result);
        void onError(String message);
    }

    public void loadQuestions(Long subtopicId, Long topicId, String difficulty, int limit, QuestionsCallback callback) {
        api.getQuestions(subtopicId, topicId, difficulty, limit).enqueue(new Callback<ApiResponse<List<SessionQuestionContentResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<SessionQuestionContentResponse>>> call,
                                   Response<ApiResponse<List<SessionQuestionContentResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<SessionQuestionContentResponse> data = response.body().getData();
                    callback.onSuccess(data != null ? data : Collections.emptyList());
                    return;
                }
                callback.onError("Khong tai duoc cau hoi luyen tap");
            }

            @Override
            public void onFailure(Call<ApiResponse<List<SessionQuestionContentResponse>>> call, Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Loi ket noi");
            }
        });
    }

    public void grade(long questionId, long selectedOptionId, GradeCallback callback) {
        api.grade(new PracticeGradeRequest(questionId, selectedOptionId)).enqueue(new Callback<ApiResponse<PracticeGradeResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<PracticeGradeResponse>> call,
                                   Response<ApiResponse<PracticeGradeResponse>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                    return;
                }
                callback.onError("Khong cham duoc cau hoi");
            }

            @Override
            public void onFailure(Call<ApiResponse<PracticeGradeResponse>> call, Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Loi ket noi");
            }
        });
    }
}
