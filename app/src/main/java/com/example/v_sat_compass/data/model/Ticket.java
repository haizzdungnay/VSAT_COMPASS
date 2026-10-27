package com.example.v_sat_compass.data.model;

public class Ticket {
    private Long id;
    private Long questionId;
    private Long reporterUserId;
    private String message;
    private String status;
    private String resolverNote;
    private String createdAt;
    private String updatedAt;

    public Long getId() { return id; }
    public Long getQuestionId() { return questionId; }
    public Long getReporterUserId() { return reporterUserId; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public String getResolverNote() { return resolverNote; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
