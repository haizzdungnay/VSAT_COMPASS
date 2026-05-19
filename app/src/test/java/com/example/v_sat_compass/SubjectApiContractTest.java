package com.example.v_sat_compass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.v_sat_compass.data.api.SubjectApi;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.SubjectResponse;

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

public class SubjectApiContractTest {

    private MockWebServer server;
    private SubjectApi api;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        api = new Retrofit.Builder()
                .baseUrl(server.url("/api/v1/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SubjectApi.class);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void getSubjects_usesCorrectGetPath() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"success\":true,\"data\":[{\"id\":1,\"code\":\"MATH\",\"name\":\"Toán học\"}]}"));

        api.getSubjects().execute();
        RecordedRequest request = server.takeRequest();

        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/subjects", request.getPath());
    }

    @Test
    public void getSubjects_successDeserializesEnvelope() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"success\":true,\"data\":[{\"id\":1,\"code\":\"MATH\",\"name\":\"Toán học\"}]}"));

        Response<ApiResponse<List<SubjectResponse>>> response = api.getSubjects().execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertTrue(response.body().isSuccess());
        List<SubjectResponse> subjects = response.body().getData();
        assertNotNull(subjects);
        assertEquals(1, subjects.size());
        assertEquals("MATH", subjects.get(0).getCode());
        assertEquals("Toán học", subjects.get(0).getName());
    }
}
