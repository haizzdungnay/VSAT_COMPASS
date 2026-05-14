package com.example.v_sat_compass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.v_sat_compass.data.api.AdminApi;
import com.example.v_sat_compass.data.model.admin.AdminExamResponse;
import com.example.v_sat_compass.data.repository.AdminExamRepository;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AdminExamRepositoryTest {

    private MockWebServer server;
    private AdminExamRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .writeTimeout(500, TimeUnit.MILLISECONDS)
                .build();
        AdminApi api = new Retrofit.Builder()
                .baseUrl(server.url("/api/v1/"))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AdminApi.class);
        repository = new AdminExamRepository(api);
    }

    @After
    public void tearDown() throws IOException {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void getExam_success_unwrapsApiResponseData() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(successEnvelope(examJson())));
        AtomicReference<AdminExamResponse> dataRef = new AtomicReference<>();
        AtomicReference<AdminExamRepository.AdminExamError> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        repository.getExam(1L, new AdminExamRepository.RepositoryCallback<AdminExamResponse>() {
            @Override
            public void onSuccess(AdminExamResponse data) {
                dataRef.set(data);
                latch.countDown();
            }

            @Override
            public void onError(AdminExamRepository.AdminExamError error) {
                errorRef.set(error);
                latch.countDown();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(dataRef.get());
        assertEquals("Sample Exam", dataRef.get().getTitle());
        assertEquals(null, errorRef.get());
    }

    @Test
    public void getExam_4xx_returnsHttpError() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody(errorEnvelope("VALIDATION_FAILED", "Bad request")));

        AdminExamRepository.AdminExamError error = captureError();

        assertEquals(AdminExamRepository.AdminExamError.Type.HTTP, error.getType());
        assertEquals(400, error.getStatusCode());
        assertEquals("VALIDATION_FAILED", error.getCode());
        assertEquals("Bad request", error.getMessage());
    }

    @Test
    public void getExam_5xx_returnsServerError() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody(errorEnvelope("INTERNAL_ERROR", "Server error")));

        AdminExamRepository.AdminExamError error = captureError();

        assertEquals(AdminExamRepository.AdminExamError.Type.SERVER, error.getType());
        assertEquals(500, error.getStatusCode());
        assertEquals("INTERNAL_ERROR", error.getCode());
        assertEquals("Server error", error.getMessage());
    }

    @Test
    public void getExam_networkFailure_returnsNetworkError() throws Exception {
        server.shutdown();
        server = null;

        AdminExamRepository.AdminExamError error = captureError();

        assertEquals(AdminExamRepository.AdminExamError.Type.NETWORK, error.getType());
        assertEquals(0, error.getStatusCode());
        assertEquals("NETWORK_FAILURE", error.getCode());
    }

    private AdminExamRepository.AdminExamError captureError() throws InterruptedException {
        AtomicReference<AdminExamRepository.AdminExamError> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        repository.getExam(1L, new AdminExamRepository.RepositoryCallback<AdminExamResponse>() {
            @Override
            public void onSuccess(AdminExamResponse data) {
                latch.countDown();
            }

            @Override
            public void onError(AdminExamRepository.AdminExamError error) {
                errorRef.set(error);
                latch.countDown();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(errorRef.get());
        return errorRef.get();
    }

    private String successEnvelope(String dataJson) {
        return "{"
                + "\"success\":true,"
                + "\"data\":" + dataJson + ","
                + "\"message\":null,"
                + "\"timestamp\":\"2026-05-15T10:00:00Z\""
                + "}";
    }

    private String errorEnvelope(String code, String message) {
        return "{"
                + "\"success\":false,"
                + "\"data\":null,"
                + "\"message\":null,"
                + "\"error\":{\"code\":\"" + code + "\",\"message\":\"" + message + "\"},"
                + "\"timestamp\":\"2026-05-15T10:00:00Z\""
                + "}";
    }

    private String examJson() {
        return "{"
                + "\"id\":1,"
                + "\"examCode\":\"ADM_MATH_001\","
                + "\"title\":\"Sample Exam\","
                + "\"subjectId\":7,"
                + "\"questionCount\":10,"
                + "\"durationMinutes\":45,"
                + "\"difficulty\":\"EASY\","
                + "\"pricingType\":\"FREE\","
                + "\"price\":0,"
                + "\"status\":\"DRAFT\","
                + "\"version\":1"
                + "}";
    }
}
