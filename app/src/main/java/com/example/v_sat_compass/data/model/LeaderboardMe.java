package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

public class LeaderboardMe {
    @SerializedName("opt_in")
    private boolean optIn;

    @SerializedName("own_entry")
    private LeaderboardEntry ownEntry;

    public boolean isOptIn() { return optIn; }
    public LeaderboardEntry getOwnEntry() { return ownEntry; }
}
