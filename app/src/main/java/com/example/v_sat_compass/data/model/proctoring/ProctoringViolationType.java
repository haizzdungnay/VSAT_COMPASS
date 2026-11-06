package com.example.v_sat_compass.data.model.proctoring;

/**
 * Types of proctoring violations during SCHEDULED_MOCK exams.
 */
public enum ProctoringViolationType {
    FOCUS_LOST,
    MULTI_WINDOW_DETECTED,
    APP_BACKGROUNDED,
    SCREEN_OVERLAY,
    LOCK_TASK_EXITED,
    FULLSCREEN_EXITED
}
