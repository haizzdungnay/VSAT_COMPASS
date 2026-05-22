package com.example.v_sat_compass.data.repository;

import com.example.v_sat_compass.data.api.AdminQuestionApi;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.admin.AdminReviewActionRequest;
import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.question.QuestionListItemResponse;
import com.example.v_sat_compass.data.model.question.QuestionResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminQuestionRepository {

    private final AdminQuestionApi api;
    private final Gson gson = new Gson();

    public AdminQuestionRepository() {
        this(ApiClient.getClient().create(AdminQuestionApi.class));
    }

    public AdminQuestionRepository(AdminQuestionApi api) {
        this.api = api;
    }

    public interface RepositoryCallback<T> {
        void onSuccess(T data);
        void onError(AdminQuestionError error);
    }

    public static class AdminQuestionError {
        public enum Type {
            HTTP,
            SERVER,
            NETWORK
        }

        private final Type type;
        private final int statusCode;
        private final String code;
        private final String message;

        public AdminQuestionError(Type type, int statusCode, String code, String message) {
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

    public void getReviewQueue(
            QuestionStatus status,
            int page,
            int size,
            RepositoryCallback<PageResponse<QuestionListItemResponse>> callback
    ) {
        enqueue(api.getReviewQueue(status, page, size), callback);
    }

    public void getQuestionDetail(Long id, RepositoryCallback<QuestionResponse> callback) {
        enqueue(api.getQuestionDetail(id), callback);
    }

    public void approve(
            Long id,
            String comment,
            RepositoryCallback<QuestionResponse> callback
    ) {
        enqueue(api.approveQuestion(id, new AdminReviewActionRequest(comment)), callback);
    }

    public void requestRevision(
            Long id,
            String comment,
            RepositoryCallback<QuestionResponse> callback
    ) {
        enqueue(api.requestRevision(id, new AdminReviewActionRequest(comment)), callback);
    }

    public void reject(
            Long id,
            String comment,
            RepositoryCallback<QuestionResponse> callback
    ) {
        enqueue(api.rejectQuestion(id, new AdminReviewActionRequest(comment)), callback);
    }

    private <T> void enqueue(Call<ApiResponse<T>> call, RepositoryCallback<T> callback) {
        call.enqueue(new Callback<ApiResponse<T>>() {
            @Override
            public void onResponse(Call<ApiResponse<T>> call, Response<ApiResponse<T>> response) {
                if (response.isSuccessful()) {
                    ApiResponse<T> body = response.body();
                    if (body != null && body.isSuccess() && body.getData() != null) {
                        callback.onSuccess(body.getData());
                    } else {
                        callback.onError(new AdminQuestionError(
                                AdminQuestionError.Type.SERVER,
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
                callback.onError(new AdminQuestionError(
                        AdminQuestionError.Type.NETWORK,
                        0,
                        "NETWORK_FAILURE",
                        t.getMessage()
                ));
            }
        });
    }

    private AdminQuestionError toHttpError(Response<?> response) {
        ApiResponse<?> errorBody = parseErrorBody(response);
        AdminQuestionError.Type type = response.code() >= 500
                ? AdminQuestionError.Type.SERVER
                : AdminQuestionError.Type.HTTP;
        return new AdminQuestionError(
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
