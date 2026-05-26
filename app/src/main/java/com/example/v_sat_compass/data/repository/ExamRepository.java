package com.example.v_sat_compass.data.repository;

import android.content.Context;

import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.ExamApi;
import com.example.v_sat_compass.data.local.LocalExamDataSource;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.data.model.PublicExamDetailResponse;
import com.example.v_sat_compass.data.model.PublicExamSummaryResponse;
import com.example.v_sat_compass.data.model.SubjectResponse;
import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

public class ExamRepository {

    private static ExamRepository instance;

    private final ExamApi examApi;
    private final SubjectRepository subjectRepository;
    private final Gson gson = new Gson();
    private Map<Long, String> subjectNameCache;

    public ExamRepository() {
        this(ApiClient.getClient().create(ExamApi.class), new SubjectRepository());
    }

    public ExamRepository(ExamApi examApi, SubjectRepository subjectRepository) {
        this.examApi = examApi;
        this.subjectRepository = subjectRepository;
    }

    public static synchronized ExamRepository getInstance() {
        if (instance == null) {
            instance = new ExamRepository();
        }
        return instance;
    }

    public interface ExamsCallback {
        void onSuccess(List<Exam> exams);
        void onError(String message);
    }

    public interface ExamCallback {
        void onSuccess(Exam exam);
        void onError(String message);
    }

    public void loadPublishedExams(Long subjectId, ExamsCallback callback) {
        try {
            Response<ApiResponse<PageResponse<PublicExamSummaryResponse>>> response =
                    examApi.getPublishedExams(subjectId, 0, 100).execute();
            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                PageResponse<PublicExamSummaryResponse> page = response.body().getData();
                List<Exam> exams = mapSummaries(page != null ? page.getContent() : null);
                callback.onSuccess(exams);
                return;
            }
            callback.onError("Không tải được danh sách đề thi");
        } catch (IOException e) {
            callback.onError(e.getMessage() != null ? e.getMessage() : "Lỗi mạng");
        }
    }

    public void loadExamDetail(long examId, ExamCallback callback) {
        try {
            Response<ApiResponse<PublicExamDetailResponse>> response =
                    examApi.getExamDetail(examId).execute();
            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                    && response.body().getData() != null) {
                callback.onSuccess(mapDetail(response.body().getData()));
                return;
            }
            callback.onError("Không tải được chi tiết đề thi");
        } catch (IOException e) {
            callback.onError(e.getMessage() != null ? e.getMessage() : "Lỗi mạng");
        }
    }

    public Exam getLocalExamDetail(Context context, long examId) {
        return LocalExamDataSource.getInstance().getExamDetail(context, examId);
    }

    public List<Exam> getLocalPublishedExams(Context context) {
        return LocalExamDataSource.getInstance().getPublishedExams(context);
    }

    public Exam findExamBySubjectName(List<Exam> exams, String subjectKeyword) {
        if (exams == null || subjectKeyword == null) {
            return null;
        }
        String keyword = subjectKeyword.toLowerCase();
        for (Exam exam : exams) {
            if (exam.getSubjectName() != null
                    && exam.getSubjectName().toLowerCase().contains(keyword)) {
                return exam;
            }
        }
        return null;
    }

    private List<Exam> mapSummaries(List<PublicExamSummaryResponse> summaries) throws IOException {
        ensureSubjectCache();
        List<Exam> exams = new ArrayList<>();
        if (summaries == null) {
            return exams;
        }
        for (PublicExamSummaryResponse summary : summaries) {
            exams.add(mapSummary(summary));
        }
        return exams;
    }

    private Exam mapSummary(PublicExamSummaryResponse summary) {
        JsonObject json = new JsonObject();
        if (summary.getId() != null) {
            json.addProperty("id", summary.getId());
        }
        if (summary.getTitle() != null) {
            json.addProperty("title", summary.getTitle());
        }
        if (summary.getDescription() != null) {
            json.addProperty("description", summary.getDescription());
        }
        if (summary.getExamCode() != null) {
            json.addProperty("exam_code", summary.getExamCode());
        }
        json.addProperty("total_questions", summary.getQuestionCount());
        json.addProperty("duration_minutes", summary.getDurationMinutes());
        if (summary.getPricingType() != null) {
            json.addProperty("pricing_type", summary.getPricingType());
        }
        if (summary.getSubjectId() != null) {
            json.addProperty("subject_id", summary.getSubjectId());
        }
        String subjectName = resolveSubjectName(summary.getSubjectId());
        if (!subjectName.isEmpty()) {
            json.addProperty("subject_name", subjectName);
        }
        return gson.fromJson(json, Exam.class);
    }

    private Exam mapDetail(PublicExamDetailResponse detail) throws IOException {
        ensureSubjectCache();
        PublicExamSummaryResponse summary = new PublicExamSummaryResponse();
        summary.setId(detail.getId());
        summary.setTitle(detail.getTitle());
        summary.setDescription(detail.getDescription());
        summary.setExamCode(detail.getExamCode());
        summary.setSubjectId(detail.getSubjectId());
        summary.setQuestionCount(detail.getQuestionCount());
        summary.setDurationMinutes(detail.getDurationMinutes());
        summary.setPricingType(detail.getPricingType());
        return mapSummary(summary);
    }

    private void ensureSubjectCache() throws IOException {
        if (subjectNameCache != null) {
            return;
        }
        subjectNameCache = new HashMap<>();
        Response<ApiResponse<List<SubjectResponse>>> response = subjectRepository.loadSubjectsSync();
        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
            List<SubjectResponse> subjects = response.body().getData();
            if (subjects != null) {
                for (SubjectResponse subject : subjects) {
                    if (subject.getId() != null) {
                        subjectNameCache.put(subject.getId(), subject.getName());
                    }
                }
            }
        }
    }

    private String resolveSubjectName(Long subjectId) {
        if (subjectId == null || subjectNameCache == null) {
            return "";
        }
        String name = subjectNameCache.get(subjectId);
        return name != null ? name : "";
    }
}
