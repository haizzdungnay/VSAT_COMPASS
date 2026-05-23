package com.example.v_sat_compass.data.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.Question;
import com.example.v_sat_compass.data.model.session.SessionAnswerKeysResponse;
import com.example.v_sat_compass.data.model.session.SessionQuestionContentResponse;

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

public class ExamApiContractTest {

    private MockWebServer server;
    private ExamApi api;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        api = new Retrofit.Builder()
                .baseUrl(server.url("/api/v1/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ExamApi.class);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void getSessionQuestionContent_usesGetSessionQuestionContentPath() throws Exception {
        enqueueData(questionJson());

        Response<ApiResponse<SessionQuestionContentResponse>> response =
                api.getSessionQuestionContent(7L, 4L).execute();
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals(4L, response.body().getData().getId());
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/sessions/7/questions/4", request.getPath());
        assertReturnTypeContains("getSessionQuestionContent", "SessionQuestionContentResponse",
                long.class, long.class);
    }

    @Test
    public void getSessionAnswerKeys_usesGetSessionAnswerKeysPath() throws Exception {
        enqueueData(answerKeysJson());

        Response<ApiResponse<SessionAnswerKeysResponse>> response =
                api.getSessionAnswerKeys(7L).execute();
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals(7L, response.body().getData().getSessionId());
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/sessions/7/answer-keys", request.getPath());
        assertReturnTypeContains("getSessionAnswerKeys", "SessionAnswerKeysResponse", long.class);
    }

    @Test
    public void legacyGetSessionQuestion_stillExists() throws Exception {
        Method method = ExamApi.class.getMethod("getSessionQuestion", Long.class, Long.class);

        assertNotNull(method.getAnnotation(GET.class));
        assertEquals("sessions/{sessionId}/questions/{questionId}",
                method.getAnnotation(GET.class).value());
        assertTrue(method.getGenericReturnType().getTypeName().contains("ApiResponse"));
        assertTrue(method.getGenericReturnType().getTypeName().contains(Question.class.getName()));
    }

    private void enqueueData(String dataJson) {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"success\":true,\"data\":" + dataJson + "}"));
    }

    private void assertReturnTypeContains(
            String methodName,
            String expected,
            Class<?>... parameterTypes
    ) throws Exception {
        Method method = ExamApi.class.getMethod(methodName, parameterTypes);
        assertTrue(method.getGenericReturnType().getTypeName().contains("ApiResponse"));
        assertTrue(method.getGenericReturnType().getTypeName().contains(expected));
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
}
