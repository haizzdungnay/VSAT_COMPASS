package com.example.v_sat_compass.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.v_sat_compass.data.api.ExamApi;
import com.example.v_sat_compass.data.model.session.SessionAnswerKeysResponse;
import com.example.v_sat_compass.data.model.session.SessionQuestionContentResponse;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SessionContentRepositoryTest {

    private MockWebServer server;
    private SessionContentRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .writeTimeout(500, TimeUnit.MILLISECONDS)
                .build();
        ExamApi api = new Retrofit.Builder()
                .baseUrl(server.url("/api/v1/"))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ExamApi.class);
        repository = new SessionContentRepository(api);
    }

    @After
    public void tearDown() throws IOException {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void getQuestion_success_returnsDto() throws Exception {
        server.enqueue(jsonResponse(200, successEnvelope(questionJson())));

        SessionQuestionContentResponse question = captureSuccess(callback ->
                repository.getQuestion(7L, 4L, callback));
        RecordedRequest request = server.takeRequest();

        assertEquals(4L, question.getId());
        assertEquals("Q-CONTENT-4", question.getQuestionCode());
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/sessions/7/questions/4", request.getPath());
    }

    @Test
    public void getQuestion_httpError_triggersFailureCallback() throws Exception {
        server.enqueue(jsonResponse(500, errorEnvelope("SERVER_ERROR", "Question failed")));

        SessionContentRepository.SessionContentError error =
                this.<SessionQuestionContentResponse>captureError(callback ->
                        repository.getQuestion(7L, 4L, callback));

        assertEquals(SessionContentRepository.SessionContentError.Type.SERVER, error.getType());
        assertEquals(500, error.getStatusCode());
        assertEquals("SERVER_ERROR", error.getCode());
        assertEquals("Question failed", error.getMessage());
    }

    @Test
    public void getAnswerKeys_success_returnsDto() throws Exception {
        server.enqueue(jsonResponse(200, successEnvelope(answerKeysJson())));

        SessionAnswerKeysResponse answerKeys = captureSuccess(callback ->
                repository.getAnswerKeys(7L, callback));
        RecordedRequest request = server.takeRequest();

        assertEquals(7L, answerKeys.getSessionId());
        assertEquals(6L, answerKeys.getExamId());
        assertEquals(4L, answerKeys.getQuestions().get(0).getQuestionId());
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/sessions/7/answer-keys", request.getPath());
    }

    @Test
    public void getAnswerKeys_httpError_triggersFailureCallback() throws Exception {
        server.enqueue(jsonResponse(400, errorEnvelope("BAD_REQUEST", "Answer keys failed")));

        SessionContentRepository.SessionContentError error =
                this.<SessionAnswerKeysResponse>captureError(callback ->
                        repository.getAnswerKeys(7L, callback));

        assertEquals(SessionContentRepository.SessionContentError.Type.HTTP, error.getType());
        assertEquals(400, error.getStatusCode());
        assertEquals("BAD_REQUEST", error.getCode());
        assertEquals("Answer keys failed", error.getMessage());
    }

    private <T> T captureSuccess(RepositoryInvocation<T> invocation) throws InterruptedException {
        AtomicReference<T> dataRef = new AtomicReference<>();
        AtomicReference<SessionContentRepository.SessionContentError> errorRef =
                new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        invocation.invoke(new SessionContentRepository.RepositoryCallback<T>() {
            @Override
            public void onSuccess(T data) {
                dataRef.set(data);
                latch.countDown();
            }

            @Override
            public void onError(SessionContentRepository.SessionContentError error) {
                errorRef.set(error);
                latch.countDown();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNull(errorRef.get());
        assertNotNull(dataRef.get());
        return dataRef.get();
    }

    private <T> SessionContentRepository.SessionContentError captureError(
            RepositoryInvocation<T> invocation
    ) throws InterruptedException {
        AtomicReference<SessionContentRepository.SessionContentError> errorRef =
                new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        invocation.invoke(new SessionContentRepository.RepositoryCallback<T>() {
            @Override
            public void onSuccess(T data) {
                latch.countDown();
            }

            @Override
            public void onError(SessionContentRepository.SessionContentError error) {
                errorRef.set(error);
                latch.countDown();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(errorRef.get());
        return errorRef.get();
    }

    private MockResponse jsonResponse(int code, String body) {
        return new MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
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
                + "\"id\":4,"
                + "\"questionCode\":\"Q-CONTENT-4\","
                + "\"content\":\"What is 2 + 2?\","
                + "\"questionType\":\"SINGLE_CHOICE\","
                + "\"difficulty\":\"EASY\","
                + "\"order\":1,"
                + "\"options\":[{\"id\":10,\"content\":\"4\",\"order\":1}]"
                + "}";
    }

    private String answerKeysJson() {
        return "{"
                + "\"sessionId\":7,"
                + "\"examId\":6,"
                + "\"questions\":[{\"questionId\":4,\"correctOptionIds\":[10],"
                + "\"explanation\":\"Basic addition\"}]"
                + "}";
    }

    private interface RepositoryInvocation<T> {
        void invoke(SessionContentRepository.RepositoryCallback<T> callback);
    }
}
