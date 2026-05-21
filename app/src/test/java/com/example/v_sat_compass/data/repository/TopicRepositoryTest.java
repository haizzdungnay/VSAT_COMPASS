package com.example.v_sat_compass.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.v_sat_compass.data.api.TopicApi;
import com.example.v_sat_compass.data.model.TopicResponse;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TopicRepositoryTest {

    private MockWebServer server;
    private TopicRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .writeTimeout(500, TimeUnit.MILLISECONDS)
                .build();
        TopicApi api = new Retrofit.Builder()
                .baseUrl(server.url("/api/v1/"))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TopicApi.class);
        repository = new TopicRepository(api);
    }

    @After
    public void tearDown() throws IOException {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void listTopics_success_unwrapsApiResponseData() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(successEnvelope("[{\"id\":2,\"subjectId\":1,\"code\":\"ALG\",\"name\":\"Algebra\"}]")));
        AtomicReference<List<TopicResponse>> dataRef = new AtomicReference<>();
        AtomicReference<TopicRepository.TopicError> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        repository.listTopics(1L, new TopicRepository.RepositoryCallback<List<TopicResponse>>() {
            @Override
            public void onSuccess(List<TopicResponse> data) {
                dataRef.set(data);
                latch.countDown();
            }

            @Override
            public void onError(TopicRepository.TopicError error) {
                errorRef.set(error);
                latch.countDown();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(dataRef.get());
        assertEquals("ALG", dataRef.get().get(0).getCode());
        assertEquals(null, errorRef.get());
    }

    @Test
    public void listTopics_4xx_returnsHttpError() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setBody(errorEnvelope("RESOURCE_NOT_FOUND", "Subject not found")));

        TopicRepository.TopicError error = captureError();

        assertEquals(TopicRepository.TopicError.Type.HTTP, error.getType());
        assertEquals(404, error.getStatusCode());
        assertEquals("RESOURCE_NOT_FOUND", error.getCode());
        assertEquals("Subject not found", error.getMessage());
    }

    @Test
    public void listTopics_5xx_returnsServerError() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody(errorEnvelope("INTERNAL_ERROR", "Server error")));

        TopicRepository.TopicError error = captureError();

        assertEquals(TopicRepository.TopicError.Type.SERVER, error.getType());
        assertEquals(500, error.getStatusCode());
        assertEquals("INTERNAL_ERROR", error.getCode());
        assertEquals("Server error", error.getMessage());
    }

    @Test
    public void listTopics_networkFailure_returnsNetworkError() throws Exception {
        server.shutdown();
        server = null;

        TopicRepository.TopicError error = captureError();

        assertEquals(TopicRepository.TopicError.Type.NETWORK, error.getType());
        assertEquals(0, error.getStatusCode());
        assertEquals("NETWORK_FAILURE", error.getCode());
    }

    private TopicRepository.TopicError captureError() throws InterruptedException {
        AtomicReference<TopicRepository.TopicError> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        repository.listTopics(1L, new TopicRepository.RepositoryCallback<List<TopicResponse>>() {
            @Override
            public void onSuccess(List<TopicResponse> data) {
                latch.countDown();
            }

            @Override
            public void onError(TopicRepository.TopicError error) {
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
}
