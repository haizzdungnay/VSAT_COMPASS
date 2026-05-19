package com.example.v_sat_compass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.v_sat_compass.data.api.SubjectApi;
import com.example.v_sat_compass.data.model.SubjectResponse;
import com.example.v_sat_compass.data.repository.SubjectRepository;

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

public class SubjectRepositoryTest {

    private MockWebServer server;
    private SubjectRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .writeTimeout(500, TimeUnit.MILLISECONDS)
                .build();
        SubjectApi api = new Retrofit.Builder()
                .baseUrl(server.url("/api/v1/"))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SubjectApi.class);
        repository = new SubjectRepository(api);
    }

    @After
    public void tearDown() throws IOException {
        if (server != null) server.shutdown();
    }

    @Test
    public void getSubjects_success_returnsSubjectList() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"success\":true,\"data\":[{\"id\":1,\"code\":\"MATH\",\"name\":\"Toán học\"}]}"));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<List<SubjectResponse>> result = new AtomicReference<>();

        repository.getSubjects(new SubjectRepository.SubjectCallback() {
            @Override public void onSuccess(List<SubjectResponse> subjects) {
                result.set(subjects);
                latch.countDown();
            }
            @Override public void onError(SubjectRepository.SubjectError error) { latch.countDown(); }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNotNull(result.get());
        assertEquals(1, result.get().size());
        assertEquals("MATH", result.get().get(0).getCode());
    }

    @Test
    public void getSubjects_4xx_returnsHttpError() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("{\"success\":false,\"code\":\"INVALID_REQUEST\",\"message\":\"Bad Request\"}"));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<SubjectRepository.SubjectError> errorRef = new AtomicReference<>();

        repository.getSubjects(new SubjectRepository.SubjectCallback() {
            @Override public void onSuccess(List<SubjectResponse> subjects) { latch.countDown(); }
            @Override public void onError(SubjectRepository.SubjectError error) {
                errorRef.set(error);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNotNull(errorRef.get());
        assertEquals(SubjectRepository.SubjectError.Type.HTTP, errorRef.get().getType());
        assertEquals(400, errorRef.get().getStatusCode());
    }

    @Test
    public void getSubjects_5xx_returnsServerError() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{\"success\":false,\"code\":\"INTERNAL\",\"message\":\"Internal Server Error\"}"));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<SubjectRepository.SubjectError> errorRef = new AtomicReference<>();

        repository.getSubjects(new SubjectRepository.SubjectCallback() {
            @Override public void onSuccess(List<SubjectResponse> subjects) { latch.countDown(); }
            @Override public void onError(SubjectRepository.SubjectError error) {
                errorRef.set(error);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNotNull(errorRef.get());
        assertEquals(SubjectRepository.SubjectError.Type.SERVER, errorRef.get().getType());
        assertEquals(500, errorRef.get().getStatusCode());
    }

    @Test
    public void getSubjects_networkFailure_returnsNetworkError() throws Exception {
        server.enqueue(new MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<SubjectRepository.SubjectError> errorRef = new AtomicReference<>();

        repository.getSubjects(new SubjectRepository.SubjectCallback() {
            @Override public void onSuccess(List<SubjectResponse> subjects) { latch.countDown(); }
            @Override public void onError(SubjectRepository.SubjectError error) {
                errorRef.set(error);
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(errorRef.get());
        assertEquals(SubjectRepository.SubjectError.Type.NETWORK, errorRef.get().getType());
    }
}
