package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

public class LeaderboardEntry {
    @SerializedName("rank")
    private int rank;

    @SerializedName("user_id")
    private Long userId;

    @SerializedName("display_name")
    private String displayName;

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("total_score")
    private long totalScore;

    @SerializedName("exam_count")
    private long examCount;

    public int getRank() { return rank; }
    public Long getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public long getTotalScore() { return totalScore; }
    public long getExamCount() { return examCount; }
}
