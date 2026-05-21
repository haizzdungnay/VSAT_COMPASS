package com.example.v_sat_compass.data.repository;

import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.TopicApi;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.SubtopicResponse;
import com.example.v_sat_compass.data.model.TopicResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TopicRepository {

    private final TopicApi api;
    private final Gson gson = new Gson();

    public TopicRepository() {
        this(ApiClient.getClient().create(TopicApi.class));
    }

    public TopicRepository(TopicApi api) {
        this.api = api;
    }

    public interface RepositoryCallback<T> {
        void onSuccess(T data);
        void onError(TopicError error);
    }

    public static class TopicError {
        public enum Type {
            HTTP,
            SERVER,
            NETWORK
        }

        private final Type type;
        private final int statusCode;
        private final String code;
        private final String message;

        public TopicError(Type type, int statusCode, String code, String message) {
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

    public void listTopics(
            Long subjectId,
            RepositoryCallback<List<TopicResponse>> callback
    ) {
        enqueue(api.listTopics(subjectId), callback);
    }

    public void listSubtopics(
            Long subjectId,
            Long topicId,
            RepositoryCallback<List<SubtopicResponse>> callback
    ) {
        enqueue(api.listSubtopics(subjectId, topicId), callback);
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
                        callback.onError(new TopicError(
                                TopicError.Type.SERVER,
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
                callback.onError(new TopicError(
                        TopicError.Type.NETWORK,
                        0,
                        "NETWORK_FAILURE",
                        t.getMessage()
                ));
            }
        });
    }

    private TopicError toHttpError(Response<?> response) {
        ApiResponse<?> errorBody = parseErrorBody(response);
        TopicError.Type type = response.code() >= 500
                ? TopicError.Type.SERVER
                : TopicError.Type.HTTP;
        return new TopicError(
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
