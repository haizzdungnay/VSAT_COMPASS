package com.example.v_sat_compass.data.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.SubtopicResponse;
import com.example.v_sat_compass.data.model.TopicResponse;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TopicApiContractTest {

    private MockWebServer server;
    private TopicApi api;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        api = new Retrofit.Builder()
                .baseUrl(server.url("/api/v1/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TopicApi.class);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void listTopics_usesSubjectTopicsPath() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"success\":true,\"data\":[{\"id\":2,\"subjectId\":1,\"code\":\"ALG\",\"name\":\"Algebra\"}]}"));

        Response<ApiResponse<List<TopicResponse>>> response = api.listTopics(1L).execute();
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals("ALG", response.body().getData().get(0).getCode());
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/subjects/1/topics", request.getPath());
    }

    @Test
    public void listSubtopics_usesSubjectTopicSubtopicsPath() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"success\":true,\"data\":[{\"id\":3,\"topicId\":2,\"code\":\"LIN\",\"name\":\"Linear\"}]}"));

        Response<ApiResponse<List<SubtopicResponse>>> response =
                api.listSubtopics(1L, 2L).execute();
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isSuccessful());
        assertEquals("LIN", response.body().getData().get(0).getCode());
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/subjects/1/topics/2/subtopics", request.getPath());
    }
}
