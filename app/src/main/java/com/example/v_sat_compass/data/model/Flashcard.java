package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

public class Flashcard {
    @SerializedName("id")
    private String id;

    @SerializedName("front")
    private String front;

    @SerializedName("back")
    private String back;

    @SerializedName("deck")
    private String deck;

    @SerializedName("subject")
    private String subject;

    private double ease;
    private int intervalDays;
    private int repetitions;
    private long nextReviewAtMillis;

    public Flashcard() {}

    public Flashcard(String id, String front, String back, String deck, String subject) {
        this.id = id;
        this.front = front;
        this.back = back;
        this.deck = deck;
        this.subject = subject;
        this.ease = FlashcardProgress.DEFAULT_EASE;
        this.intervalDays = 0;
        this.repetitions = 0;
        this.nextReviewAtMillis = 0L;
    }

    public String getId() { return id; }
    public String getFront() { return front; }
    public String getBack() { return back; }
    public String getDeck() { return deck; }
    public String getSubject() { return subject; }
    public double getEase() { return ease; }
    public int getIntervalDays() { return intervalDays; }
    public int getRepetitions() { return repetitions; }
    public long getNextReviewAtMillis() { return nextReviewAtMillis; }

    public void applyProgress(FlashcardProgress progress) {
        if (progress == null) return;
        this.ease = progress.getEase();
        this.intervalDays = progress.getIntervalDays();
        this.repetitions = progress.getRepetitions();
        this.nextReviewAtMillis = progress.getNextReviewAtMillis();
    }

    public FlashcardProgress toProgress() {
        return new FlashcardProgress(id, ease, intervalDays, repetitions, nextReviewAtMillis);
    }

    public void setProgress(double ease, int intervalDays, int repetitions, long nextReviewAtMillis) {
        this.ease = ease;
        this.intervalDays = intervalDays;
        this.repetitions = repetitions;
        this.nextReviewAtMillis = nextReviewAtMillis;
    }
}
