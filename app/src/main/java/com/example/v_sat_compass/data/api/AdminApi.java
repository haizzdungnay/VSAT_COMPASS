package com.example.v_sat_compass.data.api;

import com.example.v_sat_compass.data.model.AdminStats;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.data.model.UserItem;
import com.example.v_sat_compass.data.model.admin.AdminExamAddQuestionRequest;
import com.example.v_sat_compass.data.model.admin.AdminExamCreateRequest;
import com.example.v_sat_compass.data.model.admin.AdminExamReorderQuestionsRequest;
import com.example.v_sat_compass.data.model.admin.AdminExamResponse;
import com.example.v_sat_compass.data.model.admin.AdminExamSummaryResponse;
import com.example.v_sat_compass.data.model.admin.AdminExamUpdateRequest;
import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * API for CONTENT_ADMIN and SUPER_ADMIN.
 * Base URL already includes /api/v1/.
 */
public interface AdminApi {

    Gson LEGACY_GSON = new Gson();

    @GET("admin/stats")
    Call<ApiResponse<AdminStats>> getDashboardStats();

    @GET("admin/exams")
    Call<ApiResponse<PageResponse<AdminExamSummaryResponse>>> listAdminExams(
            @Query("status") String status,
            @Query("subjectId") Long subjectId,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("admin/exams/{id}")
    Call<ApiResponse<AdminExamResponse>> getAdminExam(@Path("id") Long id);

    @POST("admin/exams")
    Call<ApiResponse<AdminExamResponse>> createAdminExam(@Body AdminExamCreateRequest request);

    @PUT("admin/exams/{id}")
    Call<ApiResponse<AdminExamResponse>> updateAdminExam(
            @Path("id") Long id,
            @Body AdminExamUpdateRequest request
    );

    @DELETE("admin/exams/{examId}")
    Call<ApiResponse<Void>> discardDraftExam(@Path("examId") Long examId);

    @POST("admin/exams/{examId}/questions")
    Call<ApiResponse<AdminExamResponse>> addQuestionToExam(
            @Path("examId") Long examId,
            @Body AdminExamAddQuestionRequest request
    );

    @DELETE("admin/exams/{examId}/questions/{questionId}")
    Call<ApiResponse<AdminExamResponse>> removeQuestionFromExam(
            @Path("examId") Long examId,
            @Path("questionId") Long questionId
    );

    @PUT("admin/exams/{examId}/questions/reorder")
    Call<ApiResponse<AdminExamResponse>> reorderExamQuestions(
            @Path("examId") Long examId,
            @Body AdminExamReorderQuestionsRequest request
    );

    @POST("admin/exams/{examId}/submit-review")
    Call<ApiResponse<AdminExamResponse>> submitExamForReview(@Path("examId") Long examId);

    @POST("admin/exams/{examId}/publish")
    Call<ApiResponse<AdminExamResponse>> publishAdminExam(@Path("examId") Long examId);

    @POST("admin/exams/{examId}/hide")
    Call<ApiResponse<AdminExamResponse>> hideAdminExam(@Path("examId") Long examId);

    @POST("admin/exams/{examId}/archive")
    Call<ApiResponse<AdminExamResponse>> archiveAdminExam(@Path("examId") Long examId);

    @POST("admin/exams/{examId}/reject-review")
    Call<ApiResponse<AdminExamResponse>> rejectExamReview(@Path("examId") Long examId);

    @POST("admin/exams/{examId}/return-to-draft")
    Call<ApiResponse<AdminExamResponse>> returnExamToDraft(@Path("examId") Long examId);

    @GET("admin/users")
    Call<ApiResponse<PageResponse<UserItem>>> getUsers(
            @Query("role") String role,
            @Query("status") String status,
            @Query("keyword") String keyword,
            @Query("page") int page,
            @Query("size") int size
    );

    @PATCH("admin/users/{id}/role")
    Call<ApiResponse<Void>> updateUserRole(
            @Path("id") Long userId,
            @Body Map<String, String> body
    );

    @PATCH("admin/users/{id}/lock")
    Call<ApiResponse<Void>> lockUser(@Path("id") Long userId);

    @PATCH("admin/users/{id}/unlock")
    Call<ApiResponse<Void>> unlockUser(@Path("id") Long userId);

    @Deprecated
    default Call<ApiResponse<List<Exam>>> getAdminExams(
            String status,
            String subject,
            int page,
            int size
    ) {
        Type type = new TypeToken<ApiResponse<List<Exam>>>() {}.getType();
        return new TransformCall<>(
                listAdminExams(status, parseLong(subject), page, size),
                source -> source == null ? new ArrayList<>() : toLegacyExamList(source),
                type
        );
    }

    @Deprecated
    default Call<ApiResponse<Exam>> createExam(Map<String, Object> body) {
        Type type = new TypeToken<ApiResponse<Exam>>() {}.getType();
        return new TransformCall<>(
                createAdminExam(toCreateRequest(body)),
                AdminApi::toLegacyExam,
                type
        );
    }

    @Deprecated
    default Call<ApiResponse<Exam>> updateExam(Long id, Map<String, Object> body) {
        Type type = new TypeToken<ApiResponse<Exam>>() {}.getType();
        return new TransformCall<>(
                updateAdminExam(id, toUpdateRequest(body)),
                AdminApi::toLegacyExam,
                type
        );
    }

    @Deprecated
    default Call<ApiResponse<Void>> publishExam(Long id) {
        Type type = new TypeToken<ApiResponse<Void>>() {}.getType();
        return new TransformCall<>(
                publishAdminExam(id),
                source -> null,
                type
        );
    }

    static AdminExamCreateRequest toCreateRequest(Map<String, Object> body) {
        return new AdminExamCreateRequest(
                stringValue(body, "examCode", "exam_code"),
                stringValue(body, "title", null),
                longValue(body, "subjectId", "subject_id"),
                stringValue(body, "description", null),
                intValue(body, "durationMinutes", "duration_minutes"),
                stringValue(body, "difficulty", null),
                pricingTypeValue(body),
                decimalValue(body, "price", "max_score"),
                stringValue(body, "tags", null)
        );
    }

    static AdminExamUpdateRequest toUpdateRequest(Map<String, Object> body) {
        return new AdminExamUpdateRequest(
                stringValue(body, "title", null),
                longValue(body, "subjectId", "subject_id"),
                stringValue(body, "description", null),
                intValue(body, "durationMinutes", "duration_minutes"),
                stringValue(body, "difficulty", null),
                pricingTypeValue(body),
                decimalValue(body, "price", "max_score"),
                stringValue(body, "tags", null)
        );
    }

    static String pricingTypeValue(Map<String, Object> body) {
        String explicit = stringValue(body, "pricingType", "pricing_type");
        if (explicit != null) {
            return explicit;
        }
        Object isPaid = body == null ? null : body.get("is_paid");
        if (isPaid instanceof Boolean && (Boolean) isPaid) {
            return "PAID";
        }
        return "FREE";
    }

    static String stringValue(Map<String, Object> body, String camelKey, String legacyKey) {
        Object value = value(body, camelKey, legacyKey);
        return value == null ? null : String.valueOf(value);
    }

    static Long longValue(Map<String, Object> body, String camelKey, String legacyKey) {
        Object value = value(body, camelKey, legacyKey);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    static Integer intValue(Map<String, Object> body, String camelKey, String legacyKey) {
        Object value = value(body, camelKey, legacyKey);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    static BigDecimal decimalValue(Map<String, Object> body, String camelKey, String legacyKey) {
        Object value = value(body, camelKey, legacyKey);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number || value instanceof String) {
            try {
                return new BigDecimal(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    static Object value(Map<String, Object> body, String camelKey, String legacyKey) {
        if (body == null) {
            return null;
        }
        if (body.containsKey(camelKey)) {
            return body.get(camelKey);
        }
        return legacyKey == null ? null : body.get(legacyKey);
    }

    static Long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static List<Exam> toLegacyExamList(PageResponse<AdminExamSummaryResponse> page) {
        List<Exam> exams = new ArrayList<>();
        if (page.getContent() == null) {
            return exams;
        }
        for (AdminExamSummaryResponse summary : page.getContent()) {
            exams.add(toLegacyExam(summary));
        }
        return exams;
    }

    static Exam toLegacyExam(AdminExamSummaryResponse summary) {
        if (summary == null) {
            return null;
        }
        JsonObject json = new JsonObject();
        add(json, "id", summary.getId());
        add(json, "title", summary.getTitle());
        add(json, "exam_code", summary.getExamCode());
        add(json, "subject_name", summary.getSubjectId());
        add(json, "total_questions", summary.getQuestionCount());
        add(json, "duration_minutes", summary.getDurationMinutes());
        add(json, "status", summary.getStatus());
        return LEGACY_GSON.fromJson(json, Exam.class);
    }

    static Exam toLegacyExam(AdminExamResponse response) {
        if (response == null) {
            return null;
        }
        JsonObject json = new JsonObject();
        add(json, "id", response.getId());
        add(json, "title", response.getTitle());
        add(json, "description", response.getDescription());
        add(json, "exam_code", response.getExamCode());
        add(json, "subject_name", response.getSubjectId());
        add(json, "total_questions", response.getQuestionCount());
        add(json, "duration_minutes", response.getDurationMinutes());
        add(json, "status", response.getStatus());
        return LEGACY_GSON.fromJson(json, Exam.class);
    }

    static void add(JsonObject json, String name, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Number) {
            json.addProperty(name, (Number) value);
        } else if (value instanceof Boolean) {
            json.addProperty(name, (Boolean) value);
        } else {
            json.addProperty(name, String.valueOf(value));
        }
    }

    class TransformCall<S, T> implements Call<ApiResponse<T>> {
        private final Call<ApiResponse<S>> delegate;
        private final Function<S, T> mapper;
        private final Type targetEnvelopeType;

        TransformCall(
                Call<ApiResponse<S>> delegate,
                Function<S, T> mapper,
                Type targetEnvelopeType
        ) {
            this.delegate = delegate;
            this.mapper = mapper;
            this.targetEnvelopeType = targetEnvelopeType;
        }

        @Override
        public Response<ApiResponse<T>> execute() throws IOException {
            return transform(delegate.execute());
        }

        @Override
        public void enqueue(Callback<ApiResponse<T>> callback) {
            delegate.enqueue(new Callback<ApiResponse<S>>() {
                @Override
                public void onResponse(
                        Call<ApiResponse<S>> call,
                        Response<ApiResponse<S>> response
                ) {
                    callback.onResponse(TransformCall.this, transform(response));
                }

                @Override
                public void onFailure(Call<ApiResponse<S>> call, Throwable t) {
                    callback.onFailure(TransformCall.this, t);
                }
            });
        }

        @Override
        public boolean isExecuted() {
            return delegate.isExecuted();
        }

        @Override
        public void cancel() {
            delegate.cancel();
        }

        @Override
        public boolean isCanceled() {
            return delegate.isCanceled();
        }

        @Override
        public Call<ApiResponse<T>> clone() {
            return new TransformCall<>(delegate.clone(), mapper, targetEnvelopeType);
        }

        @Override
        public Request request() {
            return delegate.request();
        }

        @Override
        public Timeout timeout() {
            return delegate.timeout();
        }

        private Response<ApiResponse<T>> transform(Response<ApiResponse<S>> response) {
            if (!response.isSuccessful()) {
                return Response.error(response.errorBody(), response.raw());
            }
            return Response.success(transformBody(response.body()), response.raw());
        }

        private ApiResponse<T> transformBody(ApiResponse<S> source) {
            if (source == null) {
                return null;
            }
            JsonObject json = LEGACY_GSON.toJsonTree(source).getAsJsonObject();
            json.add("data", LEGACY_GSON.toJsonTree(mapper.apply(source.getData())));
            return LEGACY_GSON.fromJson(json, targetEnvelopeType);
        }
    }
}
