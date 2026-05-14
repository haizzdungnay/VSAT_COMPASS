package com.example.v_sat_compass.data.repository;

import com.example.v_sat_compass.data.api.AdminApi;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.admin.AdminExamAddQuestionRequest;
import com.example.v_sat_compass.data.model.admin.AdminExamCreateRequest;
import com.example.v_sat_compass.data.model.admin.AdminExamReorderQuestionsRequest;
import com.example.v_sat_compass.data.model.admin.AdminExamResponse;
import com.example.v_sat_compass.data.model.admin.AdminExamSummaryResponse;
import com.example.v_sat_compass.data.model.admin.AdminExamUpdateRequest;
import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminExamRepository {

    private final AdminApi adminApi;
    private final Gson gson = new Gson();

    public AdminExamRepository() {
        this(ApiClient.getClient().create(AdminApi.class));
    }

    public AdminExamRepository(AdminApi adminApi) {
        this.adminApi = adminApi;
    }

    public interface RepositoryCallback<T> {
        void onSuccess(T data);
        void onError(AdminExamError error);
    }

    public static class AdminExamError {
        public enum Type {
            HTTP,
            SERVER,
            NETWORK
        }

        private final Type type;
        private final int statusCode;
        private final String code;
        private final String message;

        public AdminExamError(Type type, int statusCode, String code, String message) {
            this.type = type;
            this.statusCode = statusCode;
            this.code = code;
            this.message = message;
        }

        public Type getType() {
            return type;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }

    public void listExams(
            String status,
            Long subjectId,
            int page,
            int size,
            RepositoryCallback<PageResponse<AdminExamSummaryResponse>> callback
    ) {
        enqueue(adminApi.listAdminExams(status, subjectId, page, size), callback);
    }

    public void getExam(Long id, RepositoryCallback<AdminExamResponse> callback) {
        enqueue(adminApi.getAdminExam(id), callback);
    }

    public void createExam(
            AdminExamCreateRequest request,
            RepositoryCallback<AdminExamResponse> callback
    ) {
        enqueue(adminApi.createAdminExam(request), callback);
    }

    public void updateExam(
            Long id,
            AdminExamUpdateRequest request,
            RepositoryCallback<AdminExamResponse> callback
    ) {
        enqueue(adminApi.updateAdminExam(id, request), callback);
    }

    public void discardDraftExam(Long examId, RepositoryCallback<Void> callback) {
        enqueue(adminApi.discardDraftExam(examId), callback);
    }

    public void addQuestion(
            Long examId,
            AdminExamAddQuestionRequest request,
            RepositoryCallback<AdminExamResponse> callback
    ) {
        enqueue(adminApi.addQuestionToExam(examId, request), callback);
    }

    public void removeQuestion(
            Long examId,
            Long questionId,
            RepositoryCallback<AdminExamResponse> callback
    ) {
        enqueue(adminApi.removeQuestionFromExam(examId, questionId), callback);
    }

    public void reorderQuestions(
            Long examId,
            AdminExamReorderQuestionsRequest request,
            RepositoryCallback<AdminExamResponse> callback
    ) {
        enqueue(adminApi.reorderExamQuestions(examId, request), callback);
    }

    public void submitForReview(Long examId, RepositoryCallback<AdminExamResponse> callback) {
        enqueue(adminApi.submitExamForReview(examId), callback);
    }

    public void publish(Long examId, RepositoryCallback<AdminExamResponse> callback) {
        enqueue(adminApi.publishAdminExam(examId), callback);
    }

    public void hide(Long examId, RepositoryCallback<AdminExamResponse> callback) {
        enqueue(adminApi.hideAdminExam(examId), callback);
    }

    public void archive(Long examId, RepositoryCallback<AdminExamResponse> callback) {
        enqueue(adminApi.archiveAdminExam(examId), callback);
    }

    public void rejectReview(Long examId, RepositoryCallback<AdminExamResponse> callback) {
        enqueue(adminApi.rejectExamReview(examId), callback);
    }

    public void returnToDraft(Long examId, RepositoryCallback<AdminExamResponse> callback) {
        enqueue(adminApi.returnExamToDraft(examId), callback);
    }

    private <T> void enqueue(Call<ApiResponse<T>> call, RepositoryCallback<T> callback) {
        call.enqueue(new Callback<ApiResponse<T>>() {
            @Override
            public void onResponse(Call<ApiResponse<T>> call, Response<ApiResponse<T>> response) {
                if (response.isSuccessful()) {
                    ApiResponse<T> body = response.body();
                    if (body != null && body.isSuccess()) {
                        callback.onSuccess(body.getData());
                    } else {
                        callback.onError(new AdminExamError(
                                AdminExamError.Type.SERVER,
                                response.code(),
                                errorCode(body),
                                errorMessage(body, "Empty or invalid server response")
                        ));
                    }
                    return;
                }

                callback.onError(toHttpError(response));
            }

            @Override
            public void onFailure(Call<ApiResponse<T>> call, Throwable t) {
                callback.onError(new AdminExamError(
                        AdminExamError.Type.NETWORK,
                        0,
                        "NETWORK_FAILURE",
                        t.getMessage()
                ));
            }
        });
    }

    private AdminExamError toHttpError(Response<?> response) {
        ApiResponse<?> errorBody = parseErrorBody(response);
        AdminExamError.Type type = response.code() >= 500
                ? AdminExamError.Type.SERVER
                : AdminExamError.Type.HTTP;
        return new AdminExamError(
                type,
                response.code(),
                errorCode(errorBody),
                errorMessage(errorBody, "HTTP " + response.code())
        );
    }

    private ApiResponse<?> parseErrorBody(Response<?> response) {
        if (response.errorBody() == null) {
            return null;
        }
        try {
            return gson.fromJson(response.errorBody().string(), ApiResponse.class);
        } catch (IOException | JsonSyntaxException ignored) {
            return null;
        }
    }

    private static String errorCode(ApiResponse<?> response) {
        return response != null && response.getError() != null
                ? response.getError().getCode()
                : null;
    }

    private static String errorMessage(ApiResponse<?> response, String fallback) {
        if (response != null && response.getError() != null
                && response.getError().getMessage() != null) {
            return response.getError().getMessage();
        }
        if (response != null && response.getMessage() != null) {
            return response.getMessage();
        }
        return fallback;
    }
}
