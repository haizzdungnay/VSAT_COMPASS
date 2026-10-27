package com.example.v_sat_compass.data.model;

/**
 * F9 — body for PATCH /auth/me/leaderboard.
 */
public class LeaderboardOptInRequest {
    private boolean optIn;

    public LeaderboardOptInRequest() {}
    public LeaderboardOptInRequest(boolean optIn) { this.optIn = optIn; }

    public boolean isOptIn() { return optIn; }
    public void setOptIn(boolean optIn) { this.optIn = optIn; }
}
