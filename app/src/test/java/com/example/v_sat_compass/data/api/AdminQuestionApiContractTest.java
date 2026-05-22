package com.example.v_sat_compass.data.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.admin.AdminReviewActionRequest;
import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.data.model.admin.QuestionPickerItemResponse;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.enums.QuestionType;
import com.example.v_sat_compass.data.model.question.QuestionListItemResponse;
import com.example.v_sat_compass.data.model.question.QuestionResponse;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;

public class AdminQuestionApiContractTest {

    private MockWebServer server;
    private AdminQuestionApi api;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        api = new Retrofit.Builder()
                .baseUrl(server.url("/api/v1/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AdminQuestionApi.class);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void getReviewQueue_usesGetAdminQuestionsWithStatusPageAndSize() throws Exception {
        enqueueData(pageJson());

        Response<ApiResponse<PageResponse<QuestionListItemResponse>>> response =
                api.getReviewQueue(QuestionStatus.PENDING_REVIEW, 1, 20).execute();
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals("Q-T2-ABC12345",
                response.body().getData().getContent().get(0).getQuestionCode());
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/admin/questions?status=PENDING_REVIEW&page=1&size=20",
                request.getPath());
        assertEquals(0L, request.getBody().size());
        assertReturnTypeContains("getReviewQueue", "PageResponse");
    }

    @Test
    public void getPickerQueue_usesGetAdminQuestionsPickerWithFilters() throws Exception {
        enqueueData(pickerPageJson());

        Response<ApiResponse<PageResponse<QuestionPickerItemResponse>>> response =
                api.getPickerQueue(QuestionStatus.APPROVED, 1L, 2L,
                        QuestionType.SINGLE_CHOICE, "linear", 2, 50).execute();
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals("Q-T10-PICKER",
                response.body().getData().getContent().get(0).getQuestionCode());
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/admin/questions/picker?status=APPROVED&subjectId=1"
                        + "&topicId=2&questionType=SINGLE_CHOICE&q=linear&page=2&size=50",
                request.getPath());
        assertEquals(0L, request.getBody().size());
        assertReturnTypeContains("getPickerQueue", "PageResponse");
        assertReturnTypeContains("getPickerQueue", "QuestionPickerItemResponse");
        assertNotNull(method("getPickerQueue").getAnnotation(GET.class));
    }

    @Test
    public void getQuestionDetail_usesGetAdminQuestionById() throws Exception {
        enqueueData(questionJson());

        Response<ApiResponse<QuestionResponse>> response = api.getQuestionDetail(9L).execute();
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isSuccessful());
        assertEquals("Q-T2-ABC12345", response.body().getData().getQuestionCode());
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/admin/questions/9", request.getPath());
        assertEquals(0L, request.getBody().size());
        assertReturnTypeContains("getQuestionDetail", "QuestionResponse");
    }

    @Test
    public void approveQuestion_usesPostApproveAndActionBody() throws Exception {
        enqueueData(questionJson());

        api.approveQuestion(9L, new AdminReviewActionRequest("Looks good")).execute();
        RecordedRequest request = server.takeRequest();

        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/admin/questions/9/approve", request.getPath());
        assertTrue(request.getBody().readUtf8().contains("\"comment\":\"Looks good\""));
        assertActionBodyType("approveQuestion");
    }

    @Test
    public void requestRevision_usesPostRequestRevisionAndActionBody() throws Exception {
        enqueueData(questionJson());

        api.requestRevision(9L, new AdminReviewActionRequest("Fix wording")).execute();
        RecordedRequest request = server.takeRequest();

        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/admin/questions/9/request-revision", request.getPath());
        assertTrue(request.getBody().readUtf8().contains("\"comment\":\"Fix wording\""));
        assertActionBodyType("requestRevision");
    }

    @Test
    public void rejectQuestion_usesPostRejectAndActionBody() throws Exception {
        enqueueData(questionJson());

        api.rejectQuestion(9L, new AdminReviewActionRequest("Duplicate")).execute();
        RecordedRequest request = server.takeRequest();

        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/admin/questions/9/reject", request.getPath());
        assertTrue(request.getBody().readUtf8().contains("\"comment\":\"Duplicate\""));
        assertActionBodyType("rejectQuestion");
    }

    @Test
    public void reviewActions_doNotUsePatchAnnotations() throws Exception {
        assertPostWithoutPatch("approveQuestion");
        assertPostWithoutPatch("requestRevision");
        assertPostWithoutPatch("rejectQuestion");
    }

    private void enqueueData(String dataJson) {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"success\":true,\"data\":" + dataJson + "}"));
    }

    private void assertReturnTypeContains(String methodName, String expected) throws Exception {
        Method method = method(methodName);
        assertTrue(method.getGenericReturnType().getTypeName().contains("ApiResponse"));
        assertTrue(method.getGenericReturnType().getTypeName().contains(expected));
    }

    private void assertActionBodyType(String methodName) throws Exception {
        Method method = method(methodName);
        assertEquals(AdminReviewActionRequest.class, method.getParameterTypes()[1]);
    }

    private void assertPostWithoutPatch(String methodName) throws Exception {
        Method method = method(methodName);
        assertNotNull(method.getAnnotation(POST.class));
        assertNull(method.getAnnotation(PATCH.class));
    }

    private Method method(String name) throws Exception {
        for (Method method : AdminQuestionApi.class.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new AssertionError("Missing method " + name);
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

    private String pickerPageJson() {
        return "{"
                + "\"content\":[" + pickerItemJson() + "],"
                + "\"totalElements\":1,"
                + "\"totalPages\":1,"
                + "\"number\":2,"
                + "\"size\":50"
                + "}";
    }

    private String pickerItemJson() {
        return "{"
                + "\"id\":10,"
                + "\"questionCode\":\"Q-T10-PICKER\","
                + "\"questionTextSnippet\":\"Linear equation snippet\","
                + "\"subjectId\":1,"
                + "\"topicId\":2,"
                + "\"subtopicId\":3,"
                + "\"difficulty\":\"MEDIUM\","
                + "\"questionType\":\"SINGLE_CHOICE\","
                + "\"status\":\"APPROVED\","
                + "\"version\":1,"
                + "\"updatedAt\":\"2026-05-22T10:01:00Z\","
                + "\"imageUrl\":null"
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
                + "\"status\":\"PENDING_REVIEW\","
                + "\"version\":1,"
                + "\"createdBy\":3,"
                + "\"createdAt\":\"2026-05-15T10:00:00Z\","
                + "\"updatedAt\":\"2026-05-15T10:01:00Z\","
                + "\"options\":[],"
                + "\"reviewHistory\":[]"
                + "}";
    }
}
