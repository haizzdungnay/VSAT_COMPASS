package com.example.v_sat_compass.util;

import com.example.v_sat_compass.data.model.proctoring.ProctoringViolationLog;
import com.example.v_sat_compass.data.model.proctoring.ProctoringViolationType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Proctoring monitor for anti-cheating supervision in SCHEDULED_MOCK exams.
 * - Tracks violation count (MAX_VIOLATIONS = 3)
 * - Logs each violation with timestamp and details
 * - Determines when exam should be automatically submitted (violationCount >= 3)
 */
public class ProctoringMonitor {

    public static final int MAX_VIOLATIONS = 3;

    private int violationCount = 0;
    private boolean proctoringActive = false;
    private final List<ProctoringViolationLog> violationLogs = new ArrayList<>();

    public ProctoringMonitor() {
        this(false);
    }

    public ProctoringMonitor(boolean proctoringActive) {
        this.proctoringActive = proctoringActive;
    }

    public boolean isProctoringActive() {
        return proctoringActive;
    }

    public void setProctoringActive(boolean proctoringActive) {
        this.proctoringActive = proctoringActive;
    }

    public synchronized int recordViolation(ProctoringViolationType type, String description) {
        violationCount++;
        long now = System.currentTimeMillis();
        ProctoringViolationLog log = new ProctoringViolationLog(type, now, description, violationCount);
        violationLogs.add(log);
        return violationCount;
    }

    public synchronized int recordViolation(ProctoringViolationType type) {
        return recordViolation(type, type != null ? type.name() : "VIOLATION");
    }

    public synchronized int getViolationCount() {
        return violationCount;
    }

    public synchronized List<ProctoringViolationLog> getViolationLogs() {
        return Collections.unmodifiableList(new ArrayList<>(violationLogs));
    }

    public synchronized boolean shouldAutoSubmit() {
        return violationCount >= MAX_VIOLATIONS;
    }

    public synchronized void reset() {
        violationCount = 0;
        violationLogs.clear();
    }
}
