package com.example.v_sat_compass.data.repository;

import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.CollaboratorQuestionApi;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.question.CreateQuestionRequest;
import com.example.v_sat_compass.data.model.question.QuestionListItemResponse;
import com.example.v_sat_compass.data.model.question.QuestionResponse;
import com.example.v_sat_compass.data.model.question.UpdateQuestionRequest;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CollaboratorQuestionRepository {

    private final CollaboratorQuestionApi api;
    private final Gson gson = new Gson();

    public CollaboratorQuestionRepository() {
        this(ApiClient.getClient().create(CollaboratorQuestionApi.class));
    }

    public CollaboratorQuestionRepository(CollaboratorQuestionApi api) {
        this.api = api;
    }

    public interface RepositoryCallback<T> {
        void onSuccess(T data);
        void onError(CollaboratorQuestionError error);
    }

    public static class CollaboratorQuestionError {
        public enum Type {
            HTTP,
            SERVER,
            NETWORK
        }

        private final Type type;
        private final int statusCode;
        private final String code;
        private final String message;

        public CollaboratorQuestionError(Type type, int statusCode, String code, String message) {
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

    public void createQuestion(
            CreateQuestionRequest request,
            RepositoryCallback<QuestionResponse> callback
    ) {
        enqueue(api.create(request), callback);
    }

    public void listMyQuestions(
            QuestionStatus status,
            int page,
            int size,
            RepositoryCallback<PageResponse<QuestionListItemResponse>> callback
    ) {
        enqueue(api.list(status, page, size), callback);
    }

    public void getQuestion(Long id, RepositoryCallback<QuestionResponse> callback) {
        enqueue(api.getById(id), callback);
    }

    public void updateQuestion(
            Long id,
            UpdateQuestionRequest request,
            RepositoryCallback<QuestionResponse> callback
    ) {
        enqueue(api.update(id, request), callback);
    }

    public void submitForReview(Long id, RepositoryCallback<QuestionResponse> callback) {
        enqueue(api.submitForReview(id), callback);
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
                        callback.onError(new CollaboratorQuestionError(
                                CollaboratorQuestionError.Type.SERVER,
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
                callback.onError(new CollaboratorQuestionError(
                        CollaboratorQuestionError.Type.NETWORK,
                        0,
                        "NETWORK_FAILURE",
                        t.getMessage()
                ));
            }
        });
    }

    private CollaboratorQuestionError toHttpError(Response<?> response) {
        ApiResponse<?> errorBody = parseErrorBody(response);
        CollaboratorQuestionError.Type type = response.code() >= 500
                ? CollaboratorQuestionError.Type.SERVER
                : CollaboratorQuestionError.Type.HTTP;
        return new CollaboratorQuestionError(
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
