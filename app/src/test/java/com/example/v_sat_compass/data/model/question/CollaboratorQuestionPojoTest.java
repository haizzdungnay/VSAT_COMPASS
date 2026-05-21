package com.example.v_sat_compass.data.model.question;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.v_sat_compass.data.model.enums.Difficulty;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.enums.QuestionType;
import com.example.v_sat_compass.data.model.enums.ReviewAction;
import com.google.gson.Gson;

import org.junit.Test;

import java.util.Arrays;

public class CollaboratorQuestionPojoTest {

    private final Gson gson = new Gson();

    @Test
    public void questionResponse_roundTripsNestedOptionsReviewHistoryEnumsAndDates() {
        String json = "{"
                + "\"id\":9,"
                + "\"questionCode\":\"Q-T2-ABC12345\","
                + "\"subjectId\":1,"
                + "\"topicId\":2,"
                + "\"subtopicId\":3,"
                + "\"difficulty\":\"MEDIUM\","
                + "\"questionType\":\"SINGLE_CHOICE\","
                + "\"questionText\":\"Question text\","
                + "\"questionHtml\":\"<p>Question text</p>\","
                + "\"imageUrl\":\"https://example.test/q.png\","
                + "\"explanation\":\"Explanation\","
                + "\"explanationHtml\":\"<p>Explanation</p>\","
                + "\"source\":\"Source\","
                + "\"tags\":\"tag\","
                + "\"status\":\"DRAFT\","
                + "\"version\":1,"
                + "\"createdBy\":3,"
                + "\"reviewedBy\":4,"
                + "\"createdAt\":\"2026-05-15T10:00:00Z\","
                + "\"updatedAt\":\"2026-05-15T10:01:00Z\","
                + "\"options\":[{\"id\":1,\"optionLabel\":\"A\",\"optionText\":\"A text\",\"isCorrect\":true,\"displayOrder\":1}],"
                + "\"reviewHistory\":[{\"id\":5,\"reviewerId\":4,\"action\":\"REQUEST_REVISION\",\"comment\":\"Fix\",\"versionReviewed\":1,\"createdAt\":\"2026-05-15T11:00:00Z\"}]"
                + "}";

        QuestionResponse response = gson.fromJson(json, QuestionResponse.class);
        String encoded = gson.toJson(response);

        assertEquals(QuestionStatus.DRAFT, response.getStatus());
        assertEquals(QuestionType.SINGLE_CHOICE, response.getQuestionType());
        assertEquals(Difficulty.MEDIUM, response.getDifficulty());
        assertEquals("2026-05-15T10:00:00Z", response.getCreatedAt());
        assertEquals(Boolean.TRUE, response.getOptions().get(0).getIsCorrect());
        assertEquals(ReviewAction.REQUEST_REVISION, response.getReviewHistory().get(0).getAction());
        assertTrue(encoded.contains("\"reviewHistory\""));
    }

    @Test
    public void questionListItemResponse_roundTripsEnumsAndDate() {
        QuestionListItemResponse item = new QuestionListItemResponse(
                9L,
                "Q-T2-ABC12345",
                1L,
                2L,
                Difficulty.HARD,
                QuestionType.MULTIPLE_CHOICE,
                QuestionStatus.PENDING_REVIEW,
                2,
                3L,
                "2026-05-15T10:01:00Z"
        );

        String json = gson.toJson(item);
        QuestionListItemResponse decoded = gson.fromJson(json, QuestionListItemResponse.class);

        assertTrue(json.contains("\"questionType\":\"MULTIPLE_CHOICE\""));
        assertEquals(QuestionStatus.PENDING_REVIEW, decoded.getStatus());
        assertEquals("2026-05-15T10:01:00Z", decoded.getUpdatedAt());
    }

    @Test
    public void questionOptionResponse_roundTripsFields() {
        QuestionOptionResponse option = new QuestionOptionResponse(
                1L,
                "A",
                "A text",
                "<p>A text</p>",
                "https://example.test/a.png",
                true,
                1
        );

        QuestionOptionResponse decoded = gson.fromJson(gson.toJson(option), QuestionOptionResponse.class);

        assertEquals("A", decoded.getOptionLabel());
        assertEquals(Boolean.TRUE, decoded.getIsCorrect());
        assertEquals(Integer.valueOf(1), decoded.getDisplayOrder());
    }

    @Test
    public void questionReviewResponse_roundTripsActionAndDate() {
        QuestionReviewResponse review = new QuestionReviewResponse(
                5L,
                4L,
                ReviewAction.APPROVE,
                "Looks good",
                2,
                "2026-05-15T11:00:00Z"
        );

        QuestionReviewResponse decoded = gson.fromJson(gson.toJson(review), QuestionReviewResponse.class);

        assertEquals(ReviewAction.APPROVE, decoded.getAction());
        assertEquals("2026-05-15T11:00:00Z", decoded.getCreatedAt());
    }

    @Test
    public void questionOptionInput_serializesRuntimeFields() {
        QuestionOptionInput option = new QuestionOptionInput("A", "A text", null, null, true, 1);

        String json = gson.toJson(option);

        assertTrue(json.contains("\"optionLabel\":\"A\""));
        assertTrue(json.contains("\"optionText\":\"A text\""));
        assertTrue(json.contains("\"isCorrect\":true"));
        assertTrue(json.contains("\"displayOrder\":1"));
    }

    @Test
    public void createQuestionRequest_serializesOptionsAndEnums() {
        CreateQuestionRequest request = new CreateQuestionRequest(
                1L,
                2L,
                3L,
                Difficulty.EASY,
                QuestionType.TRUE_FALSE,
                "Question text",
                null,
                null,
                "Explanation",
                null,
                "Source",
                "tag",
                Arrays.asList(
                        new QuestionOptionInput("A", "True", null, null, true, 1),
                        new QuestionOptionInput("B", "False", null, null, false, 2)
                )
        );

        String json = gson.toJson(request);

        assertTrue(json.contains("\"difficulty\":\"EASY\""));
        assertTrue(json.contains("\"questionType\":\"TRUE_FALSE\""));
        assertTrue(json.contains("\"options\""));
    }

    @Test
    public void updateQuestionRequest_serializesOptionsAndNullableFields() {
        UpdateQuestionRequest request = new UpdateQuestionRequest(
                null,
                2L,
                null,
                Difficulty.VERY_HARD,
                QuestionType.FILL_IN_BLANK,
                "Updated text",
                null,
                null,
                "Updated explanation",
                null,
                null,
                "tag",
                Arrays.asList(new QuestionOptionInput("A", "Answer", null, null, true, 1))
        );

        String json = gson.toJson(request);

        assertTrue(json.contains("\"topicId\":2"));
        assertTrue(json.contains("\"difficulty\":\"VERY_HARD\""));
        assertTrue(json.contains("\"questionType\":\"FILL_IN_BLANK\""));
        assertTrue(json.contains("\"options\""));
    }
}
