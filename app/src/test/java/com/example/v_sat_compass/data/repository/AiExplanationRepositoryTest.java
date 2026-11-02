package com.example.v_sat_compass.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.v_sat_compass.data.api.AiApi;
import com.example.v_sat_compass.data.model.AiExplainRequest;
import com.example.v_sat_compass.data.model.AiExplainResponse;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AiExplanationRepositoryTest {

    private AiApi mockApi;
    private AiExplanationRepository repository;
    private final Gson gson = new Gson();

    @Before
    public void setUp() {
        mockApi = mock(AiApi.class);
        repository = new AiExplanationRepository(mockApi);
    }

    @Test
    public void explain_success_callsOnSuccess() {
        Call<ApiResponse<AiExplainResponse>> mockCall = mock(Call.class);
        when(mockApi.explain(any(AiExplainRequest.class))).thenReturn(mockCall);

        String successJson = "{\"data\":{\"questionId\":101,\"explanation\":\"Step 1: calculate derivative...\",\"cached\":false}}";
        Type type = new TypeToken<ApiResponse<AiExplainResponse>>() {}.getType();
        ApiResponse<AiExplainResponse> apiResponse = gson.fromJson(successJson, type);

        ArgumentCaptor<Callback<ApiResponse<AiExplainResponse>>> captor =
                ArgumentCaptor.forClass(Callback.class);

        AtomicReference<AiExplainResponse> successRef = new AtomicReference<>();
        AtomicReference<String> errorRef = new AtomicReference<>();

        repository.explain(101L, 202L, new AiExplanationRepository.ExplainCallback() {
            @Override
            public void onSuccess(AiExplainResponse response) {
                successRef.set(response);
            }

            @Override
            public void onError(String message) {
                errorRef.set(message);
            }
        });

        verify(mockCall).enqueue(captor.capture());
        captor.getValue().onResponse(mockCall, Response.success(apiResponse));

        assertNotNull(successRef.get());
        assertEquals("Step 1: calculate derivative...", successRef.get().getExplanation());
        assertEquals(101L, successRef.get().getQuestionId());
    }

    @Test
    public void explain_503NotConfigured_mapsToFriendlyMessage() {
        Call<ApiResponse<AiExplainResponse>> mockCall = mock(Call.class);
        when(mockApi.explain(any(AiExplainRequest.class))).thenReturn(mockCall);

        ArgumentCaptor<Callback<ApiResponse<AiExplainResponse>>> captor =
                ArgumentCaptor.forClass(Callback.class);

        AtomicReference<String> errorRef = new AtomicReference<>();

        repository.explain(101L, 202L, new AiExplanationRepository.ExplainCallback() {
            @Override
            public void onSuccess(AiExplainResponse response) {}

            @Override
            public void onError(String message) {
                errorRef.set(message);
            }
        });

        verify(mockCall).enqueue(captor.capture());
        String json = "{\"error\":{\"code\":\"AI_NOT_CONFIGURED\",\"message\":\"LLM not set\"}}";
        ResponseBody errorBody = ResponseBody.create(
                MediaType.parse("application/json"),
                json
        );
        captor.getValue().onResponse(mockCall, Response.error(503, errorBody));

        assertTrue(errorRef.get().contains("AI chua duoc cau hinh tren backend"));
    }

    @Test
    public void explain_networkFailure_mapsToFriendlyMessage() {
        Call<ApiResponse<AiExplainResponse>> mockCall = mock(Call.class);
        when(mockApi.explain(any(AiExplainRequest.class))).thenReturn(mockCall);

        ArgumentCaptor<Callback<ApiResponse<AiExplainResponse>>> captor =
                ArgumentCaptor.forClass(Callback.class);

        AtomicReference<String> errorRef = new AtomicReference<>();

        repository.explain(101L, 202L, new AiExplanationRepository.ExplainCallback() {
            @Override
            public void onSuccess(AiExplainResponse response) {}

            @Override
            public void onError(String message) {
                errorRef.set(message);
            }
        });

        verify(mockCall).enqueue(captor.capture());
        captor.getValue().onFailure(mockCall, new java.io.IOException("Connection refused"));

        assertTrue(errorRef.get().contains("Khong the ket noi den AI - kiem tra mang"));
    }
}
