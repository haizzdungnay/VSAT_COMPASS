package com.example.v_sat_compass.data.model;

public class TicketUpdateRequest {
    private String status;
    private String resolverNote;

    public TicketUpdateRequest(String status, String resolverNote) {
        this.status = status;
        this.resolverNote = resolverNote;
    }
}
