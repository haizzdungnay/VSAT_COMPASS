package com.example.v_sat_compass.data.model;

import static org.junit.Assert.assertEquals;

import com.example.v_sat_compass.data.model.session.QuestionAnswerKeyResponse;
import com.example.v_sat_compass.data.model.session.QuestionOptionContentResponse;
import com.example.v_sat_compass.data.model.session.SessionAnswerKeysResponse;
import com.example.v_sat_compass.data.model.session.SessionQuestionContentResponse;
import com.google.gson.Gson;

import org.junit.Test;

public class SessionContentResponseTest {

    private final Gson gson = new Gson();

    @Test
    public void sessionQuestionContentResponse_mapsJsonFields() {
        SessionQuestionContentResponse response = gson.fromJson("{"
                        + "\"id\":4,"
                        + "\"questionCode\":\"Q-CONTENT-4\","
                        + "\"content\":\"What is 2 + 2?\","
                        + "\"questionType\":\"SINGLE_CHOICE\","
                        + "\"difficulty\":\"EASY\","
                        + "\"order\":1,"
                        + "\"options\":[{\"id\":10,\"content\":\"4\",\"order\":1}]"
                        + "}",
                SessionQuestionContentResponse.class);

        assertEquals(4L, response.getId());
        assertEquals("Q-CONTENT-4", response.getQuestionCode());
        assertEquals("What is 2 + 2?", response.getContent());
        assertEquals("SINGLE_CHOICE", response.getQuestionType());
        assertEquals("EASY", response.getDifficulty());
        assertEquals(1, response.getOrder());
        assertEquals(10L, response.getOptions().get(0).getId());
    }

    @Test
    public void questionOptionContentResponse_mapsJsonFields() {
        QuestionOptionContentResponse response = gson.fromJson("{"
                        + "\"id\":10,"
                        + "\"content\":\"4\","
                        + "\"order\":1"
                        + "}",
                QuestionOptionContentResponse.class);

        assertEquals(10L, response.getId());
        assertEquals("4", response.getContent());
        assertEquals(1, response.getOrder());
    }

    @Test
    public void sessionAnswerKeysResponse_mapsJsonFields() {
        SessionAnswerKeysResponse response = gson.fromJson("{"
                        + "\"sessionId\":7,"
                        + "\"examId\":6,"
                        + "\"questions\":[{\"questionId\":4,\"correctOptionIds\":[10],"
                        + "\"explanation\":\"Basic addition\"}]"
                        + "}",
                SessionAnswerKeysResponse.class);

        assertEquals(7L, response.getSessionId());
        assertEquals(6L, response.getExamId());
        assertEquals(4L, response.getQuestions().get(0).getQuestionId());
    }

    @Test
    public void questionAnswerKeyResponse_mapsJsonFields() {
        QuestionAnswerKeyResponse response = gson.fromJson("{"
                        + "\"questionId\":4,"
                        + "\"correctOptionIds\":[10],"
                        + "\"explanation\":\"Basic addition\""
                        + "}",
                QuestionAnswerKeyResponse.class);

        assertEquals(4L, response.getQuestionId());
        assertEquals(Long.valueOf(10L), response.getCorrectOptionIds().get(0));
        assertEquals("Basic addition", response.getExplanation());
    }
}
