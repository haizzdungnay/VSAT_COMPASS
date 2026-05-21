package com.example.v_sat_compass.data.model.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

public class AdminReviewActionRequestPojoTest {

    private final Gson gson = new Gson();

    @Test
    public void jsonSerialization_usesCommentFieldName() {
        String json = gson.toJson(new AdminReviewActionRequest("Review note"));

        assertTrue(json.contains("\"comment\":\"Review note\""));
    }

    @Test
    public void nullComment_usesProjectGsonNullOmissionPatternAndReadsAsNull() {
        String json = gson.toJson(new AdminReviewActionRequest(null));
        AdminReviewActionRequest parsed = gson.fromJson("{}", AdminReviewActionRequest.class);

        assertFalse(json.contains("comment"));
        assertNull(parsed.getComment());
    }

    @Test
    public void constructorGetterAndSetter_roundTripComment() {
        AdminReviewActionRequest request = new AdminReviewActionRequest("Initial");

        request.setComment("Updated");

        assertEquals("Updated", request.getComment());
    }

    @Test
    public void commentLengthBoundary_isPojoOnlyAndCanHold2001Characters() {
        AdminReviewActionRequest boundary = new AdminReviewActionRequest(repeat('a', 2000));
        AdminReviewActionRequest tooLong = new AdminReviewActionRequest(repeat('b', 2001));

        assertEquals(2000, boundary.getComment().length());
        assertEquals(2001, tooLong.getComment().length());
    }

    private static String repeat(char value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
