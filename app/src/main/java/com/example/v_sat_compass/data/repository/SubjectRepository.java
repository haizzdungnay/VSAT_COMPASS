package com.example.v_sat_compass.data.repository;

import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.SubjectApi;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.SubjectResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubjectRepository {

    private final SubjectApi subjectApi;
    private final Gson gson = new Gson();

    public SubjectRepository() {
        this(ApiClient.getClient().create(SubjectApi.class));
    }

    public SubjectRepository(SubjectApi subjectApi) {
        this.subjectApi = subjectApi;
    }

    public interface SubjectCallback {
        void onSuccess(List<SubjectResponse> subjects);
        void onError(SubjectError error);
    }

    public static class SubjectError {
        public enum Type {
            HTTP,
            SERVER,
            NETWORK
        }

        private final Type type;
        private final int statusCode;
        private final String code;
        private final String message;

        public SubjectError(Type type, int statusCode, String code, String message) {
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

    public Response<ApiResponse<List<SubjectResponse>>> loadSubjectsSync() throws IOException {
        return subjectApi.getSubjects().execute();
    }

    public void getSubjects(SubjectCallback callback) {
        subjectApi.getSubjects().enqueue(new Callback<ApiResponse<List<SubjectResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<SubjectResponse>>> call,
                                   Response<ApiResponse<List<SubjectResponse>>> response) {
                if (response.isSuccessful()) {
                    ApiResponse<List<SubjectResponse>> body = response.body();
                    if (body != null && body.isSuccess()) {
                        callback.onSuccess(body.getData());
                    } else {
                        callback.onError(new SubjectError(
                                SubjectError.Type.SERVER,
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
            public void onFailure(Call<ApiResponse<List<SubjectResponse>>> call, Throwable t) {
                callback.onError(new SubjectError(
                        SubjectError.Type.NETWORK,
                        0,
                        "NETWORK_FAILURE",
                        t.getMessage()
                ));
            }
        });
    }

    private SubjectError toHttpError(Response<?> response) {
        ApiResponse<?> errorBody = parseErrorBody(response);
        SubjectError.Type type = response.code() >= 500
                ? SubjectError.Type.SERVER
                : SubjectError.Type.HTTP;
        return new SubjectError(
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
