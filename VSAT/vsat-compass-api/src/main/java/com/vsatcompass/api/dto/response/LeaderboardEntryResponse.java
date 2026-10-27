package com.vsatcompass.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * F9 — single row of the public leaderboard. Tied to a user that has explicitly
 * opted in (leaderboard_opt_in = true). We never include email/phone here.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardEntryResponse {

    /** 1-based rank, computed after sorting by total score desc. */
    private int rank;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    /** Sum of exam_sessions.score (V-SAT points) for this user. */
    @JsonProperty("total_score")
    private long totalScore;

    @JsonProperty("exam_count")
    private long examCount;
}
