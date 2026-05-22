package com.example.v_sat_compass.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.v_sat_compass.data.api.AdminQuestionApi;
import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.question.QuestionListItemResponse;
import com.example.v_sat_compass.data.model.question.QuestionResponse;

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

public class AdminQuestionRepositoryTest {

    private MockWebServer server;
    private AdminQuestionRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .writeTimeout(500, TimeUnit.MILLISECONDS)
                .build();
        AdminQuestionApi api = new Retrofit.Builder()
                .baseUrl(server.url("/api/v1/"))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AdminQuestionApi.class);
        repository = new AdminQuestionRepository(api);
    }

    @After
    public void tearDown() throws IOException {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void getReviewQueue_success_unwrapsPageData() throws Exception {
        server.enqueue(jsonResponse(200, successEnvelope(pageJson())));

        PageResponse<QuestionListItemResponse> page = captureSuccess(callback ->
                repository.getReviewQueue(QuestionStatus.PENDING_REVIEW, 0, 20, callback));
        RecordedRequest request = server.takeRequest();

        assertEquals("Q-T2-ABC12345", page.getContent().get(0).getQuestionCode());
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/admin/questions?status=PENDING_REVIEW&page=0&size=20",
                request.getPath());
    }

    @Test
    public void getReviewQueue_httpError_returnsParsedError() throws Exception {
        server.enqueue(jsonResponse(400, errorEnvelope("VALIDATION_FAILED", "Bad status")));

        AdminQuestionRepository.AdminQuestionError error =
                this.<PageResponse<QuestionListItemResponse>>captureError(callback ->
                repository.getReviewQueue(QuestionStatus.DRAFT, 0, 20, callback));

        assertEquals(AdminQuestionRepository.AdminQuestionError.Type.HTTP, error.getType());
        assertEquals(400, error.getStatusCode());
        assertEquals("VALIDATION_FAILED", error.getCode());
        assertEquals("Bad status", error.getMessage());
    }

    @Test
    public void getReviewQueue_networkFailure_returnsNetworkError() throws Exception {
        server.shutdown();
        server = null;

        AdminQuestionRepository.AdminQuestionError error =
                this.<PageResponse<QuestionListItemResponse>>captureError(callback ->
                repository.getReviewQueue(null, 0, 20, callback));

        assertEquals(AdminQuestionRepository.AdminQuestionError.Type.NETWORK, error.getType());
        assertEquals(0, error.getStatusCode());
        assertEquals("NETWORK_FAILURE", error.getCode());
    }

    @Test
    public void getReviewQueue_nullBody_returnsServerError() throws Exception {
        server.enqueue(jsonResponse(200, "null"));

        AdminQuestionRepository.AdminQuestionError error =
                this.<PageResponse<QuestionListItemResponse>>captureError(callback ->
                repository.getReviewQueue(null, 0, 20, callback));

        assertEquals(AdminQuestionRepository.AdminQuestionError.Type.SERVER, error.getType());
        assertEquals("Empty or invalid server response", error.getMessage());
    }

    @Test
    public void getQuestionDetail_success_unwrapsQuestionData() throws Exception {
        server.enqueue(jsonResponse(200, successEnvelope(questionJson())));

        QuestionResponse question = captureSuccess(callback ->
                repository.getQuestionDetail(9L, callback));

        assertEquals("Q-T2-ABC12345", question.getQuestionCode());
        assertEquals(QuestionStatus.PENDING_REVIEW, question.getStatus());
    }

    @Test
    public void reviewActions_success_postCommentBodies() throws Exception {
        server.enqueue(jsonResponse(200, successEnvelope(questionJson())));
        server.enqueue(jsonResponse(200, successEnvelope(questionJson())));
        server.enqueue(jsonResponse(200, successEnvelope(questionJson())));

        QuestionResponse approved =
                this.<QuestionResponse>captureSuccess(callback ->
                        repository.approve(9L, null, callback));
        assertEquals("Q-T2-ABC12345", approved.getQuestionCode());
        RecordedRequest approve = server.takeRequest();
        assertEquals("POST", approve.getMethod());
        assertEquals("/api/v1/admin/questions/9/approve", approve.getPath());
        assertEquals("{}", approve.getBody().readUtf8());

        this.<QuestionResponse>captureSuccess(callback ->
                repository.requestRevision(9L, "Fix", callback));
        RecordedRequest revision = server.takeRequest();
        assertEquals("/api/v1/admin/questions/9/request-revision", revision.getPath());
        assertTrue(revision.getBody().readUtf8().contains("\"comment\":\"Fix\""));

        this.<QuestionResponse>captureSuccess(callback ->
                repository.reject(9L, "Duplicate", callback));
        RecordedRequest reject = server.takeRequest();
        assertEquals("/api/v1/admin/questions/9/reject", reject.getPath());
        assertTrue(reject.getBody().readUtf8().contains("\"comment\":\"Duplicate\""));
    }

    @Test
    public void reviewAction_httpError_returnsParsedError() throws Exception {
        server.enqueue(jsonResponse(409, errorEnvelope("INVALID_STATE", "Already reviewed")));

        AdminQuestionRepository.AdminQuestionError error =
                this.<QuestionResponse>captureError(callback ->
                repository.reject(9L, "Duplicate", callback));

        assertEquals(AdminQuestionRepository.AdminQuestionError.Type.HTTP, error.getType());
        assertEquals(409, error.getStatusCode());
        assertEquals("INVALID_STATE", error.getCode());
        assertEquals("Already reviewed", error.getMessage());
    }

    private <T> T captureSuccess(RepositoryInvocation<T> invocation) throws InterruptedException {
        AtomicReference<T> dataRef = new AtomicReference<>();
        AtomicReference<AdminQuestionRepository.AdminQuestionError> errorRef =
                new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        invocation.invoke(new AdminQuestionRepository.RepositoryCallback<T>() {
            @Override
            public void onSuccess(T data) {
                dataRef.set(data);
                latch.countDown();
            }

            @Override
            public void onError(AdminQuestionRepository.AdminQuestionError error) {
                errorRef.set(error);
                latch.countDown();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNull(errorRef.get());
        assertNotNull(dataRef.get());
        return dataRef.get();
    }

    private <T> AdminQuestionRepository.AdminQuestionError captureError(
            RepositoryInvocation<T> invocation
    ) throws InterruptedException {
        AtomicReference<AdminQuestionRepository.AdminQuestionError> errorRef =
                new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        invocation.invoke(new AdminQuestionRepository.RepositoryCallback<T>() {
            @Override
            public void onSuccess(T data) {
                latch.countDown();
            }

            @Override
            public void onError(AdminQuestionRepository.AdminQuestionError error) {
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

    private String pageJson() {
        return "{"
                + "\"content\":[" + listItemJson() + "],"
                + "\"totalElements\":1,"
                + "\"totalPages\":1,"
                + "\"number\":0,"
                + "\"size\":20"
                + "}";
    }

    private String listItemJson() {
        return "{"
                + "\"id\":9,"
                + "\"questionCode\":\"Q-T2-ABC12345\","
                + "\"subjectId\":1,"
                + "\"topicId\":2,"
                + "\"difficulty\":\"MEDIUM\","
                + "\"questionType\":\"SINGLE_CHOICE\","
                + "\"status\":\"PENDING_REVIEW\","
                + "\"version\":1,"
                + "\"createdBy\":3,"
                + "\"updatedAt\":\"2026-05-15T10:01:00Z\""
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
                + "\"status\":\"PENDING_REVIEW\","
                + "\"version\":1,"
                + "\"createdBy\":3,"
                + "\"createdAt\":\"2026-05-15T10:00:00Z\","
                + "\"updatedAt\":\"2026-05-15T10:01:00Z\","
                + "\"options\":[],"
                + "\"reviewHistory\":[]"
                + "}";
    }

    private interface RepositoryInvocation<T> {
        void invoke(AdminQuestionRepository.RepositoryCallback<T> callback);
    }
}
