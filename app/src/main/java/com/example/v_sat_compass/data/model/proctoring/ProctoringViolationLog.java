package com.example.v_sat_compass.data.model.proctoring;

import com.google.gson.annotations.SerializedName;

/**
 * Log entry for a single anti-cheating / proctoring violation.
 */
public class ProctoringViolationLog {

    @SerializedName("type")
    private final ProctoringViolationType type;

    @SerializedName("timestamp")
    private final long timestamp;

    @SerializedName("description")
    private final String description;

    @SerializedName("violationNumber")
    private final int violationNumber;

    public ProctoringViolationLog(ProctoringViolationType type, long timestamp, String description, int violationNumber) {
        this.type = type;
        this.timestamp = timestamp;
        this.description = description;
        this.violationNumber = violationNumber;
    }

    public ProctoringViolationType getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public String getDescription() { return description; }
    public int getViolationNumber() { return violationNumber; }
}
