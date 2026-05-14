package com.example.v_sat_compass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.v_sat_compass.data.model.admin.AdminExamCreateRequest;
import com.example.v_sat_compass.data.model.admin.AdminExamResponse;
import com.example.v_sat_compass.data.model.admin.AdminExamSummaryResponse;
import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.lang.reflect.Type;
import java.math.BigDecimal;

public class AdminExamPojoTest {

    private final Gson gson = new Gson();

    @Test
    public void createRequest_serializesRuntimeContractFields() {
        AdminExamCreateRequest request = new AdminExamCreateRequest(
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

        String json = gson.toJson(request);

        assertTrue(json.contains("\"examCode\":\"ADM_MATH_001\""));
        assertTrue(json.contains("\"subjectId\":7"));
        assertTrue(json.contains("\"durationMinutes\":45"));
        assertTrue(json.contains("\"pricingType\":\"FREE\""));
    }

    @Test
    public void adminExamResponse_deserializesRuntimeContractFields() {
        String json = "{"
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

        AdminExamResponse response = gson.fromJson(json, AdminExamResponse.class);

        assertEquals(Long.valueOf(1L), response.getId());
        assertEquals("ADM_MATH_001", response.getExamCode());
        assertEquals("Sample Exam", response.getTitle());
        assertEquals(Long.valueOf(7L), response.getSubjectId());
        assertEquals("DRAFT", response.getStatus());
        assertEquals(Integer.valueOf(10), response.getQuestionCount());
    }

    @Test
    public void pageResponse_deserializesSpringPageShape() {
        String json = "{"
                + "\"content\":[{"
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
                + "}],"
                + "\"totalElements\":1,"
                + "\"totalPages\":1,"
                + "\"number\":0,"
                + "\"size\":20"
                + "}";
        Type type = new TypeToken<PageResponse<AdminExamSummaryResponse>>() {}.getType();

        PageResponse<AdminExamSummaryResponse> page = gson.fromJson(json, type);

        assertEquals(1L, page.getTotalElements());
        assertEquals(1, page.getTotalPages());
        assertEquals("ADM_MATH_001", page.getContent().get(0).getExamCode());
    }
}
