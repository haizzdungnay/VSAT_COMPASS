package com.example.v_sat_compass.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

public class TopicPojoTest {

    private final Gson gson = new Gson();

    @Test
    public void topicResponse_roundTripsBackendFields() {
        TopicResponse topic = new TopicResponse(2L, 1L, "ALG", "Algebra", "Desc", 1);

        String json = gson.toJson(topic);
        TopicResponse decoded = gson.fromJson(json, TopicResponse.class);

        assertTrue(json.contains("\"subjectId\":1"));
        assertEquals("ALG", decoded.getCode());
        assertEquals(Integer.valueOf(1), decoded.getDisplayOrder());
    }

    @Test
    public void subtopicResponse_roundTripsBackendFields() {
        SubtopicResponse subtopic = new SubtopicResponse(3L, 2L, "LIN", "Linear", "Desc", 1);

        String json = gson.toJson(subtopic);
        SubtopicResponse decoded = gson.fromJson(json, SubtopicResponse.class);

        assertTrue(json.contains("\"topicId\":2"));
        assertEquals("LIN", decoded.getCode());
        assertEquals(Integer.valueOf(1), decoded.getDisplayOrder());
    }
}
