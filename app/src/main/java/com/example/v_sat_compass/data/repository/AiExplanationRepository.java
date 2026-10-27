package com.example.v_sat_compass.data.repository;

import com.example.v_sat_compass.data.api.AiApi;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.model.AiExplainRequest;
import com.example.v_sat_compass.data.model.AiExplainResponse;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiExplanationRepository {

    private final AiApi api;
    private final Gson gson = new Gson();

    public AiExplanationRepository() {
        this(ApiClient.getClient().create(AiApi.class));
    }

    public AiExplanationRepository(AiApi api) {
        this.api = api;
    }

    public interface ExplainCallback {
        void onSuccess(AiExplainResponse response);
        void onError(String message);
    }

    public void explain(long questionId, Long selectedOptionId, ExplainCallback callback) {
        api.explain(new AiExplainRequest(questionId, selectedOptionId))
                .enqueue(new Callback<ApiResponse<AiExplainResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AiExplainResponse>> call,
                                           Response<ApiResponse<AiExplainResponse>> response) {
                        if (response.isSuccessful()) {
                            ApiResponse<AiExplainResponse> body = response.body();
                            if (body != null && body.isSuccess() && body.getData() != null) {
                                callback.onSuccess(body.getData());
                                return;
                            }
                            callback.onError("AI chua tra ve noi dung");
                            return;
                        }
                        callback.onError(toFriendlyMessage(response));
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AiExplainResponse>> call, Throwable t) {
                        callback.onError("Khong the ket noi den AI - kiem tra mang");
                    }
                });
    }

    /**
     * Map HTTP error responses to user-friendly Vietnamese messages.
     * 401/403 = auth/role issue (different from a network problem).
     * 503    = AI service not configured on the backend.
     * 5xx    = backend problem.
     */
    private String toFriendlyMessage(Response<?> response) {
        ApiResponse<?> errorBody = parseErrorBody(response);
        String serverMessage = errorMessage(errorBody);
        int status = response.code();
        String code = errorCode(errorBody);

        if (status == 401) {
            return serverMessage != null
                    ? "Phien dang nhap het han: " + serverMessage
                    : "Phien dang nhap het han - vui long dang nhap lai";
        }
        if (status == 403) {
            return serverMessage != null
                    ? "Tai khoan khong co quyen: " + serverMessage
                    : "Tai khoan hien tai khong co quyen dung AI";
        }
        if ("AI_NOT_CONFIGURED".equalsIgnoreCase(code) || status == 503) {
            return "AI chua duoc cau hinh tren backend";
        }
        if (status >= 500) {
            return "Backend loi (HTTP " + status + ") - vui long thu lai sau";
        }
        return "Khong lay duoc giai thich AI (HTTP " + status + ")";
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

    private static String errorMessage(ApiResponse<?> response) {
        if (response == null || response.getError() == null) return null;
        return response.getError().getMessage();
    }
}
