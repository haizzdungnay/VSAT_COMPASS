package com.example.v_sat_compass.data.repository;

import com.example.v_sat_compass.data.model.Flashcard;
import com.example.v_sat_compass.data.model.FlashcardProgress;

public final class FlashcardScheduler {
    public enum Rating {
        AGAIN,
        HARD,
        GOOD,
        EASY
    }

    private static final double MIN_EASE = 1.3;
    private static final long MINUTE_MILLIS = 60_000L;
    private static final long DAY_MILLIS = 86_400_000L;

    private FlashcardScheduler() {}

    public static FlashcardProgress review(Flashcard card, Rating rating, long nowMillis) {
        double ease = card != null ? card.getEase() : FlashcardProgress.DEFAULT_EASE;
        int interval = card != null ? card.getIntervalDays() : 0;
        int repetitions = card != null ? card.getRepetitions() : 0;
        String cardId = card != null ? card.getId() : "";

        switch (rating) {
            case AGAIN:
                ease = Math.max(MIN_EASE, ease - 0.20);
                return new FlashcardProgress(cardId, ease, 0, 0, nowMillis + 10 * MINUTE_MILLIS);
            case HARD:
                ease = Math.max(MIN_EASE, ease - 0.15);
                interval = interval <= 0 ? 1 : Math.max(1, (int) Math.ceil(interval * 1.2));
                repetitions += 1;
                break;
            case EASY:
                ease += 0.15;
                interval = nextEasyInterval(interval, repetitions, ease);
                repetitions += 1;
                break;
            case GOOD:
            default:
                interval = nextGoodInterval(interval, repetitions, ease);
                repetitions += 1;
                break;
        }

        long nextReview = nowMillis + Math.max(1, interval) * DAY_MILLIS;
        return new FlashcardProgress(cardId, ease, interval, repetitions, nextReview);
    }

    private static int nextGoodInterval(int interval, int repetitions, double ease) {
        if (repetitions <= 0) return 1;
        if (repetitions == 1) return 6;
        return Math.max(1, (int) Math.round(Math.max(1, interval) * ease));
    }

    private static int nextEasyInterval(int interval, int repetitions, double ease) {
        if (repetitions <= 0) return 4;
        if (repetitions == 1) return 7;
        return Math.max(1, (int) Math.round(Math.max(1, interval) * (ease + 0.15)));
    }
}
