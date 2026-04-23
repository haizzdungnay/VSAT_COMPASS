package com.example.v_sat_compass.data.model;

/** Hằng số điểm số V-SAT dùng chung toàn app. */
public final class ScoreConstants {

    private ScoreConstants() {}

    /** Thang điểm tối đa V-SAT (1200 điểm). */
    public static final int VSAT_MAX_SCORE = 1200;

    /** Hệ số chuyển đổi từ phần trăm sang thang V-SAT (1200/100 = 12). */
    public static final int PERCENT_TO_VSAT = VSAT_MAX_SCORE / 100;

    /** Ngưỡng chủ đề "cần cải thiện" (dưới 60%). */
    public static final int WEAK_TOPIC_THRESHOLD_PERCENT = 60;
}
