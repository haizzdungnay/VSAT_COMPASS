package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

public class AdminStats {

    @SerializedName("pending_questions")
    private int pendingQuestions;

    @SerializedName("revenue_today")
    private long revenueToday;

    @SerializedName("error_tickets")
    private int errorTickets;

    @SerializedName("total_exams")
    private int totalExams;

    @SerializedName("total_users")
    private int totalUsers;

    @SerializedName("total_sessions_today")
    private int totalSessionsToday;

    public int getPendingQuestions() { return pendingQuestions; }
    public long getRevenueToday()    { return revenueToday; }
    public int getErrorTickets()     { return errorTickets; }
    public int getTotalExams()       { return totalExams; }
    public int getTotalUsers()       { return totalUsers; }
    public int getTotalSessionsToday() { return totalSessionsToday; }
}
