package com.example.v_sat_compass.data.model;

public class TicketCreateRequest {
    private Long questionId;
    private String message;

    public TicketCreateRequest(Long questionId, String message) {
        this.questionId = questionId;
        this.message = message;
    }
}
