package com.example.v_sat_compass;

import com.example.v_sat_compass.data.model.Question;
import com.example.v_sat_compass.data.model.enums.QuestionType;
import com.example.v_sat_compass.data.model.session.SessionQuestionContentResponse;
import com.google.gson.Gson;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for V-SAT 2026 4 question formats on Android Client.
 */
public class VsatQuestionFormatsTest {

    @Test
    public void testQuestionTypeEnumContainsAllVsat2026Formats() {
        assertNotNull(QuestionType.valueOf("TRUE_FALSE"));
        assertNotNull(QuestionType.valueOf("SINGLE_CHOICE"));
        assertNotNull(QuestionType.valueOf("MATCHING"));
        assertNotNull(QuestionType.valueOf("SHORT_ANSWER"));
    }

    @Test
    public void testQuestionModelSerializesQuestionType() {
        Gson gson = new Gson();
        String json = "{\"id\":101,\"question_code\":\"MATH-TF-001\",\"question_type\":\"TRUE_FALSE\",\"question_text\":\"Menh de a, b, c, d\"}";

        Question q = gson.fromJson(json, Question.class);
        assertNotNull(q);
        assertEquals("MATH-TF-001", q.getQuestionCode());
        assertEquals("TRUE_FALSE", q.getQuestionType());
    }

    @Test
    public void testSessionQuestionContentResponseDeserializesVsatFormats() {
        Gson gson = new Gson();
        String json = "{\"id\":202,\"questionCode\":\"MATH-MATCH-001\",\"questionType\":\"MATCHING\",\"content\":\"Ghep cap\"}";

        SessionQuestionContentResponse bq = gson.fromJson(json, SessionQuestionContentResponse.class);
        assertNotNull(bq);
        assertEquals("MATCHING", bq.getQuestionType());
        assertEquals("MATH-MATCH-001", bq.getQuestionCode());
    }

    @Test
    public void testShortAnswerModelResolution() {
        Question q = new Question();
        q.setQuestionType("SHORT_ANSWER");
        assertEquals("SHORT_ANSWER", q.getQuestionType());
    }

    @Test
    public void testAnswerPayloadJsonSerialization() {
        Gson gson = new Gson();

        Map<String, Object> tfPayload = new HashMap<>();
        tfPayload.put("a", true);
        tfPayload.put("b", false);
        String tfJson = gson.toJson(tfPayload);
        assertTrue(tfJson.contains("\"a\":true"));
        assertTrue(tfJson.contains("\"b\":false"));

        Map<String, Object> matchPayload = new HashMap<>();
        matchPayload.put("1", "A");
        matchPayload.put("2", "C");
        String matchJson = gson.toJson(matchPayload);
        assertTrue(matchJson.contains("\"1\":\"A\""));
        assertTrue(matchJson.contains("\"2\":\"C\""));

        Map<String, Object> saPayload = new HashMap<>();
        saPayload.put("value", "37");
        String saJson = gson.toJson(saPayload);
        assertEquals("{\"value\":\"37\"}", saJson);
    }
}
// ponytail: focused test for V-SAT 2026 formats on Android. Add instrumented UI tests when emulator farm enabled.
