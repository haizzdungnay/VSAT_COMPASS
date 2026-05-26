package com.example.v_sat_compass.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.v_sat_compass.data.api.ExamApi;
import com.example.v_sat_compass.data.api.SubjectApi;
import com.example.v_sat_compass.data.model.Exam;

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

public class ExamRepositoryTest {

    private MockWebServer server;
    private ExamRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .writeTimeout(500, TimeUnit.MILLISECONDS)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(server.url("/api/v1/"))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ExamApi examApi = retrofit.create(ExamApi.class);
        SubjectApi subjectApi = retrofit.create(SubjectApi.class);
        repository = new ExamRepository(examApi, new SubjectRepository(subjectApi));
    }

    @After
    public void tearDown() throws IOException {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void loadPublishedExams_mapsPageFieldsAndSubjectName() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"success\":true,\"data\":{"
                        + "\"content\":[{\"id\":10,\"title\":\"Đề mẫu\",\"examCode\":\"EX-10\","
                        + "\"subjectId\":1,\"questionCount\":40,\"durationMinutes\":90,\"pricingType\":\"FREE\"}],"
                        + "\"totalElements\":1,\"totalPages\":1,\"number\":0,\"size\":100"
                        + "}}"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"success\":true,\"data\":[{\"id\":1,\"code\":\"MATH\",\"name\":\"Toán học\"}]}"));

        AtomicReference<List<Exam>> examsRef = new AtomicReference<>();
        AtomicReference<String> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        repository.loadPublishedExams(null, new ExamRepository.ExamsCallback() {
            @Override
            public void onSuccess(List<Exam> exams) {
                examsRef.set(exams);
                latch.countDown();
            }

            @Override
            public void onError(String message) {
                errorRef.set(message);
                latch.countDown();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNull(errorRef.get());
        assertNotNull(examsRef.get());
        assertEquals(1, examsRef.get().size());

        Exam exam = examsRef.get().get(0);
        assertEquals(Long.valueOf(10L), exam.getId());
        assertEquals("Đề mẫu", exam.getTitle());
        assertEquals("EX-10", exam.getExamCode());
        assertEquals(40, exam.getTotalQuestions());
        assertEquals(90, exam.getDurationMinutes());
        assertEquals("FREE", exam.getPricingType());
        assertEquals(Long.valueOf(1L), exam.getSubjectId());
        assertEquals("Toán học", exam.getSubjectName());
    }

    @Test
    public void loadExamDetail_mapsSummaryFields() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"success\":true,\"data\":{"
                        + "\"id\":5,\"title\":\"Đề chi tiết\",\"examCode\":\"EX-5\","
                        + "\"subjectId\":2,\"questionCount\":25,\"durationMinutes\":60,\"pricingType\":\"PAID\""
                        + "}}"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"success\":true,\"data\":[{\"id\":2,\"code\":\"LIT\",\"name\":\"Ngữ văn\"}]}"));

        AtomicReference<Exam> examRef = new AtomicReference<>();
        AtomicReference<String> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        repository.loadExamDetail(5L, new ExamRepository.ExamCallback() {
            @Override
            public void onSuccess(Exam exam) {
                examRef.set(exam);
                latch.countDown();
            }

            @Override
            public void onError(String message) {
                errorRef.set(message);
                latch.countDown();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNull(errorRef.get());
        assertNotNull(examRef.get());
        assertEquals(Long.valueOf(5L), examRef.get().getId());
        assertEquals("Đề chi tiết", examRef.get().getTitle());
        assertEquals(25, examRef.get().getTotalQuestions());
        assertEquals("PAID", examRef.get().getPricingType());
        assertEquals("Ngữ văn", examRef.get().getSubjectName());
    }
}
