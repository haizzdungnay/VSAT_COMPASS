package com.vsatcompass.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatsResponse {

    @JsonProperty("pending_questions")
    private int pendingQuestions;

    @JsonProperty("revenue_today")
    private long revenueToday;

    @JsonProperty("error_tickets")
    private int errorTickets;

    @JsonProperty("total_exams")
    private int totalExams;

    @JsonProperty("total_users")
    private int totalUsers;

    @JsonProperty("total_sessions_today")
    private int totalSessionsToday;

    @JsonProperty("sessions_last_7_days")
    private int[] sessionsLast7Days;
}
