package com.example.v_sat_compass.data.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.data.model.enums.Difficulty;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.enums.QuestionType;
import com.example.v_sat_compass.data.model.question.CreateQuestionRequest;
import com.example.v_sat_compass.data.model.question.QuestionListItemResponse;
import com.example.v_sat_compass.data.model.question.QuestionOptionInput;
import com.example.v_sat_compass.data.model.question.QuestionResponse;
import com.example.v_sat_compass.data.model.question.UpdateQuestionRequest;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CollaboratorQuestionApiContractTest {

    private MockWebServer server;
    private CollaboratorQuestionApi api;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        api = new Retrofit.Builder()
                .baseUrl(server.url("/api/v1/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CollaboratorQuestionApi.class);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void create_usesPostRootAndBody() throws Exception {
        enqueueData(questionJson());

        Response<ApiResponse<QuestionResponse>> response = api.create(createRequest()).execute();
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals("Q-T2-ABC12345", response.body().getData().getQuestionCode());
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/collaborator/questions", request.getPath());
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"questionText\":\"Question text\""));
        assertTrue(body.contains("\"options\""));
    }

    @Test
    public void list_usesGetRootWithPagingAndStatusQuery() throws Exception {
        enqueueData(pageJson());

        Response<ApiResponse<PageResponse<QuestionListItemResponse>>> response =
                api.list(QuestionStatus.PENDING_REVIEW, 1, 20).execute();
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isSuccessful());
        assertEquals("Q-T2-ABC12345", response.body().getData().getContent().get(0).getQuestionCode());
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/collaborator/questions?status=PENDING_REVIEW&page=1&size=20",
                request.getPath());
        assertEquals(0L, request.getBody().size());
    }

    @Test
    public void getById_usesGetByIdWithoutBody() throws Exception {
        enqueueData(questionJson());

        Response<ApiResponse<QuestionResponse>> response = api.getById(9L).execute();
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isSuccessful());
        assertEquals(QuestionStatus.DRAFT, response.body().getData().getStatus());
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/collaborator/questions/9", request.getPath());
        assertEquals(0L, request.getBody().size());
    }

    @Test
    public void update_usesPutByIdAndBody() throws Exception {
        enqueueData(questionJson());

        Response<ApiResponse<QuestionResponse>> response =
                api.update(9L, updateRequest()).execute();
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isSuccessful());
        assertEquals("PUT", request.getMethod());
        assertEquals("/api/v1/collaborator/questions/9", request.getPath());
        assertTrue(request.getBody().readUtf8().contains("\"questionText\":\"Updated text\""));
    }

    @Test
    public void submitForReview_usesPostSubmitForReviewWithoutBody() throws Exception {
        enqueueData(questionJson());

        Response<ApiResponse<QuestionResponse>> response = api.submitForReview(9L).execute();
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isSuccessful());
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/collaborator/questions/9/submit-for-review", request.getPath());
        assertEquals(0L, request.getBody().size());
    }

    private void enqueueData(String dataJson) {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"success\":true,\"data\":" + dataJson + "}"));
    }

    private String pageJson() {
        return "{"
                + "\"content\":[" + listItemJson() + "],"
                + "\"totalElements\":1,"
                + "\"totalPages\":1,"
                + "\"number\":1,"
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
                + "\"subtopicId\":3,"
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

    private CreateQuestionRequest createRequest() {
        return new CreateQuestionRequest(
                1L,
                2L,
                3L,
                Difficulty.MEDIUM,
                QuestionType.SINGLE_CHOICE,
                "Question text",
                null,
                null,
                "Explanation",
                null,
                "Source",
                "tag",
                Arrays.asList(
                        new QuestionOptionInput("A", "A text", null, null, true, 1),
                        new QuestionOptionInput("B", "B text", null, null, false, 2)
                )
        );
    }

    private UpdateQuestionRequest updateRequest() {
        return new UpdateQuestionRequest(
                1L,
                2L,
                3L,
                Difficulty.HARD,
                QuestionType.SINGLE_CHOICE,
                "Updated text",
                null,
                null,
                "Updated explanation",
                null,
                "Updated source",
                "tag",
                Arrays.asList(
                        new QuestionOptionInput("A", "A text", null, null, true, 1),
                        new QuestionOptionInput("B", "B text", null, null, false, 2)
                )
        );
    }
}
