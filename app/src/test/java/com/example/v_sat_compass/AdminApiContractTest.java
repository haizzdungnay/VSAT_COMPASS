package com.example.v_sat_compass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.v_sat_compass.data.api.AdminApi;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.admin.AdminExamAddQuestionRequest;
import com.example.v_sat_compass.data.model.admin.AdminExamCreateRequest;
import com.example.v_sat_compass.data.model.admin.AdminExamReorderQuestionsRequest;
import com.example.v_sat_compass.data.model.admin.AdminExamResponse;
import com.example.v_sat_compass.data.model.admin.AdminExamSummaryResponse;
import com.example.v_sat_compass.data.model.admin.AdminExamUpdateRequest;
import com.example.v_sat_compass.data.model.admin.PageResponse;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AdminApiContractTest {

    private MockWebServer server;
    private AdminApi api;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        api = new Retrofit.Builder()
                .baseUrl(server.url("/api/v1/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AdminApi.class);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void listAdminExams_usesGetRootAndDeserializesEnvelope() throws Exception {
        enqueueData(pageJson());

        Response<ApiResponse<PageResponse<AdminExamSummaryResponse>>> response =
                api.listAdminExams("DRAFT", 7L, 0, 20).execute();
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals("ADM_MATH_001", response.body().getData().getContent().get(0).getExamCode());
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/admin/exams?status=DRAFT&subjectId=7&page=0&size=20",
                request.getPath());
    }

    @Test
    public void getAdminExam_usesGetByIdAndDeserializesEnvelope() throws Exception {
        enqueueData(examJson());

        Response<ApiResponse<AdminExamResponse>> response = api.getAdminExam(1L).execute();
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isSuccessful());
        assertEquals("Sample Exam", response.body().getData().getTitle());
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/admin/exams/1", request.getPath());
    }

    @Test
    public void createAdminExam_usesPostRootAndBody() throws Exception {
        enqueueData(examJson());

        Response<ApiResponse<AdminExamResponse>> response = api.createAdminExam(createRequest())
                .execute();
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isSuccessful());
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/admin/exams", request.getPath());
        assertTrue(request.getBody().readUtf8().contains("\"examCode\":\"ADM_MATH_001\""));
    }

    @Test
    public void updateAdminExam_usesPutByIdAndBody() throws Exception {
        enqueueData(examJson());

        Response<ApiResponse<AdminExamResponse>> response =
                api.updateAdminExam(1L, updateRequest()).execute();
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isSuccessful());
        assertEquals("PUT", request.getMethod());
        assertEquals("/api/v1/admin/exams/1", request.getPath());
        assertTrue(request.getBody().readUtf8().contains("\"title\":\"Updated Exam\""));
    }

    @Test
    public void discardDraftExam_usesDeleteByExamId() throws Exception {
        enqueueData("null");

        Response<ApiResponse<Void>> response = api.discardDraftExam(1L).execute();
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isSuccessful());
        assertEquals("DELETE", request.getMethod());
        assertEquals("/api/v1/admin/exams/1", request.getPath());
    }

    @Test
    public void addQuestionToExam_usesPostQuestionsAndBody() throws Exception {
        enqueueData(examJson());

        Response<ApiResponse<AdminExamResponse>> response =
                api.addQuestionToExam(1L, new AdminExamAddQuestionRequest(99L)).execute();
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isSuccessful());
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/admin/exams/1/questions", request.getPath());
        assertTrue(request.getBody().readUtf8().contains("\"questionId\":99"));
    }

    @Test
    public void removeQuestionFromExam_usesDeleteQuestionPath() throws Exception {
        enqueueData(examJson());

        Response<ApiResponse<AdminExamResponse>> response =
                api.removeQuestionFromExam(1L, 99L).execute();
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isSuccessful());
        assertEquals("DELETE", request.getMethod());
        assertEquals("/api/v1/admin/exams/1/questions/99", request.getPath());
    }

    @Test
    public void reorderExamQuestions_usesPutReorderAndBody() throws Exception {
        enqueueData(examJson());

        Response<ApiResponse<AdminExamResponse>> response = api.reorderExamQuestions(
                1L,
                new AdminExamReorderQuestionsRequest(Arrays.asList(3L, 2L, 1L))
        ).execute();
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isSuccessful());
        assertEquals("PUT", request.getMethod());
        assertEquals("/api/v1/admin/exams/1/questions/reorder", request.getPath());
        assertTrue(request.getBody().readUtf8().contains("\"questionIds\":[3,2,1]"));
    }

    @Test
    public void submitExamForReview_usesPostSubmitReview() throws Exception {
        enqueueData(examJson());

        assertPostWorkflow(api.submitExamForReview(1L).execute(), "/api/v1/admin/exams/1/submit-review");
    }

    @Test
    public void publishAdminExam_usesPostPublish() throws Exception {
        enqueueData(examJson());

        assertPostWorkflow(api.publishAdminExam(1L).execute(), "/api/v1/admin/exams/1/publish");
    }

    @Test
    public void hideAdminExam_usesPostHide() throws Exception {
        enqueueData(examJson());

        assertPostWorkflow(api.hideAdminExam(1L).execute(), "/api/v1/admin/exams/1/hide");
    }

    @Test
    public void archiveAdminExam_usesPostArchive() throws Exception {
        enqueueData(examJson());

        assertPostWorkflow(api.archiveAdminExam(1L).execute(), "/api/v1/admin/exams/1/archive");
    }

    @Test
    public void rejectExamReview_usesPostRejectReview() throws Exception {
        enqueueData(examJson());

        assertPostWorkflow(api.rejectExamReview(1L).execute(), "/api/v1/admin/exams/1/reject-review");
    }

    @Test
    public void returnExamToDraft_usesPostReturnToDraft() throws Exception {
        enqueueData(examJson());

        assertPostWorkflow(api.returnExamToDraft(1L).execute(), "/api/v1/admin/exams/1/return-to-draft");
    }

    private void assertPostWorkflow(
            Response<ApiResponse<AdminExamResponse>> response,
            String expectedPath
    ) throws InterruptedException {
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals("DRAFT", response.body().getData().getStatus());
        assertEquals("POST", request.getMethod());
        assertEquals(expectedPath, request.getPath());
    }

    private void enqueueData(String dataJson) {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(envelope(dataJson)));
    }

    private String envelope(String dataJson) {
        return "{"
                + "\"success\":true,"
                + "\"data\":" + dataJson + ","
                + "\"message\":null,"
                + "\"timestamp\":\"2026-05-15T10:00:00Z\""
                + "}";
    }

    private String pageJson() {
        return "{"
                + "\"content\":[" + summaryJson() + "],"
                + "\"totalElements\":1,"
                + "\"totalPages\":1,"
                + "\"number\":0,"
                + "\"size\":20"
                + "}";
    }

    private String summaryJson() {
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
                + "\"version\":1,"
                + "\"updatedAt\":\"2026-05-15T10:01:00Z\""
                + "}";
    }

    private String examJson() {
        return "{"
                + "\"id\":1,"
                + "\"examCode\":\"ADM_MATH_001\","
                + "\"title\":\"Sample Exam\","
                + "\"subjectId\":7,"
                + "\"subjectCode\":\"MATH\","
                + "\"description\":\"Description\","
                + "\"questionCount\":10,"
                + "\"durationMinutes\":45,"
                + "\"difficulty\":\"EASY\","
                + "\"pricingType\":\"FREE\","
                + "\"price\":0,"
                + "\"status\":\"DRAFT\","
                + "\"tags\":\"math\","
                + "\"version\":1,"
                + "\"createdBy\":100,"
                + "\"reviewedBy\":101,"
                + "\"createdAt\":\"2026-05-15T10:00:00Z\","
                + "\"updatedAt\":\"2026-05-15T10:01:00Z\""
                + "}";
    }

    private AdminExamCreateRequest createRequest() {
        return new AdminExamCreateRequest(
                "ADM_MATH_001",
                "Sample Exam",
                7L,
                "Description",
                45,
                "EASY",
                "FREE",
                BigDecimal.ZERO,
                "math"
        );
    }

    private AdminExamUpdateRequest updateRequest() {
        return new AdminExamUpdateRequest(
                "Updated Exam",
                7L,
                "Updated Description",
                60,
                "MEDIUM",
                "FREE",
                BigDecimal.ZERO,
                "math"
        );
    }
}
