package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

public class FlashcardProgress {
    public static final double DEFAULT_EASE = 2.5;

    @SerializedName("card_id")
    private String cardId;

    @SerializedName("ease")
    private double ease = DEFAULT_EASE;

    @SerializedName("interval_days")
    private int intervalDays;

    @SerializedName("repetitions")
    private int repetitions;

    @SerializedName("next_review_at_millis")
    private long nextReviewAtMillis;

    public FlashcardProgress() {}

    public FlashcardProgress(String cardId, double ease, int intervalDays,
                             int repetitions, long nextReviewAtMillis) {
        this.cardId = cardId;
        this.ease = ease;
        this.intervalDays = intervalDays;
        this.repetitions = repetitions;
        this.nextReviewAtMillis = nextReviewAtMillis;
    }

    public String getCardId() { return cardId; }
    public double getEase() { return ease > 0 ? ease : DEFAULT_EASE; }
    public int getIntervalDays() { return Math.max(0, intervalDays); }
    public int getRepetitions() { return Math.max(0, repetitions); }
    public long getNextReviewAtMillis() { return Math.max(0L, nextReviewAtMillis); }
}
