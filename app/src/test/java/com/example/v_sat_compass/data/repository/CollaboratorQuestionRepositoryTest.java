package com.example.v_sat_compass.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.v_sat_compass.data.api.CollaboratorQuestionApi;
import com.example.v_sat_compass.data.model.question.QuestionResponse;

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

public class CollaboratorQuestionRepositoryTest {

    private MockWebServer server;
    private CollaboratorQuestionRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .writeTimeout(500, TimeUnit.MILLISECONDS)
                .build();
        CollaboratorQuestionApi api = new Retrofit.Builder()
                .baseUrl(server.url("/api/v1/"))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CollaboratorQuestionApi.class);
        repository = new CollaboratorQuestionRepository(api);
    }

    @After
    public void tearDown() throws IOException {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void getQuestion_success_unwrapsApiResponseData() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(successEnvelope(questionJson())));
        AtomicReference<QuestionResponse> dataRef = new AtomicReference<>();
        AtomicReference<CollaboratorQuestionRepository.CollaboratorQuestionError> errorRef =
                new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        repository.getQuestion(9L, new CollaboratorQuestionRepository.RepositoryCallback<QuestionResponse>() {
            @Override
            public void onSuccess(QuestionResponse data) {
                dataRef.set(data);
                latch.countDown();
            }

            @Override
            public void onError(CollaboratorQuestionRepository.CollaboratorQuestionError error) {
                errorRef.set(error);
                latch.countDown();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(dataRef.get());
        assertEquals("Q-T2-ABC12345", dataRef.get().getQuestionCode());
        assertEquals(null, errorRef.get());
    }

    @Test
    public void getQuestion_4xx_returnsHttpError() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody(errorEnvelope("VALIDATION_FAILED", "Bad request")));

        CollaboratorQuestionRepository.CollaboratorQuestionError error = captureError();

        assertEquals(CollaboratorQuestionRepository.CollaboratorQuestionError.Type.HTTP, error.getType());
        assertEquals(400, error.getStatusCode());
        assertEquals("VALIDATION_FAILED", error.getCode());
        assertEquals("Bad request", error.getMessage());
    }

    @Test
    public void getQuestion_5xx_returnsServerError() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody(errorEnvelope("INTERNAL_ERROR", "Server error")));

        CollaboratorQuestionRepository.CollaboratorQuestionError error = captureError();

        assertEquals(CollaboratorQuestionRepository.CollaboratorQuestionError.Type.SERVER, error.getType());
        assertEquals(500, error.getStatusCode());
        assertEquals("INTERNAL_ERROR", error.getCode());
        assertEquals("Server error", error.getMessage());
    }

    @Test
    public void getQuestion_networkFailure_returnsNetworkError() throws Exception {
        server.shutdown();
        server = null;

        CollaboratorQuestionRepository.CollaboratorQuestionError error = captureError();

        assertEquals(CollaboratorQuestionRepository.CollaboratorQuestionError.Type.NETWORK, error.getType());
        assertEquals(0, error.getStatusCode());
        assertEquals("NETWORK_FAILURE", error.getCode());
    }

    private CollaboratorQuestionRepository.CollaboratorQuestionError captureError()
            throws InterruptedException {
        AtomicReference<CollaboratorQuestionRepository.CollaboratorQuestionError> errorRef =
                new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        repository.getQuestion(9L, new CollaboratorQuestionRepository.RepositoryCallback<QuestionResponse>() {
            @Override
            public void onSuccess(QuestionResponse data) {
                latch.countDown();
            }

            @Override
            public void onError(CollaboratorQuestionRepository.CollaboratorQuestionError error) {
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
                + "\"message\":null"
                + "}";
    }

    private String errorEnvelope(String code, String message) {
        return "{"
                + "\"success\":false,"
                + "\"data\":null,"
                + "\"message\":null,"
                + "\"error\":{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}"
                + "}";
    }

    private String questionJson() {
        return "{"
                + "\"id\":9,"
                + "\"questionCode\":\"Q-T2-ABC12345\","
                + "\"subjectId\":1,"
                + "\"topicId\":2,"
                + "\"difficulty\":\"MEDIUM\","
                + "\"questionType\":\"SINGLE_CHOICE\","
                + "\"questionText\":\"Question text\","
                + "\"status\":\"DRAFT\","
                + "\"version\":1,"
                + "\"createdBy\":3,"
                + "\"createdAt\":\"2026-05-15T10:00:00Z\","
                + "\"updatedAt\":\"2026-05-15T10:01:00Z\","
                + "\"options\":[],"
                + "\"reviewHistory\":[]"
                + "}";
    }
}
