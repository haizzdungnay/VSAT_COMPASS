package com.example.v_sat_compass.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.v_sat_compass.data.model.Flashcard;
import com.example.v_sat_compass.data.model.FlashcardProgress;

import org.junit.Test;

public class FlashcardSchedulerTest {
    private static final long NOW = 1_700_000_000_000L;
    private static final long DAY = 86_400_000L;

    @Test
    public void againReturnsInTenMinutesAndResetsRepetitions() {
        Flashcard card = new Flashcard("c1", "front", "back", "deck", "subject");
        card.setProgress(2.5, 6, 2, NOW);

        FlashcardProgress progress = FlashcardScheduler.review(
                card, FlashcardScheduler.Rating.AGAIN, NOW);

        assertEquals(0, progress.getIntervalDays());
        assertEquals(0, progress.getRepetitions());
        assertEquals(NOW + 10 * 60_000L, progress.getNextReviewAtMillis());
        assertTrue(progress.getEase() < 2.5);
    }

    @Test
    public void goodFirstReviewSchedulesTomorrow() {
        Flashcard card = new Flashcard("c1", "front", "back", "deck", "subject");

        FlashcardProgress progress = FlashcardScheduler.review(
                card, FlashcardScheduler.Rating.GOOD, NOW);

        assertEquals(1, progress.getIntervalDays());
        assertEquals(1, progress.getRepetitions());
        assertEquals(NOW + DAY, progress.getNextReviewAtMillis());
    }

    @Test
    public void goodRepeatedReviewExpandsByEase() {
        Flashcard card = new Flashcard("c1", "front", "back", "deck", "subject");
        card.setProgress(2.5, 6, 2, NOW);

        FlashcardProgress progress = FlashcardScheduler.review(
                card, FlashcardScheduler.Rating.GOOD, NOW);

        assertEquals(15, progress.getIntervalDays());
        assertEquals(3, progress.getRepetitions());
        assertEquals(NOW + 15 * DAY, progress.getNextReviewAtMillis());
    }

    @Test
    public void easyIsLongerThanGood() {
        Flashcard goodCard = new Flashcard("c1", "front", "back", "deck", "subject");
        Flashcard easyCard = new Flashcard("c1", "front", "back", "deck", "subject");
        goodCard.setProgress(2.5, 6, 2, NOW);
        easyCard.setProgress(2.5, 6, 2, NOW);

        FlashcardProgress good = FlashcardScheduler.review(
                goodCard, FlashcardScheduler.Rating.GOOD, NOW);
        FlashcardProgress easy = FlashcardScheduler.review(
                easyCard, FlashcardScheduler.Rating.EASY, NOW);

        assertTrue(easy.getIntervalDays() > good.getIntervalDays());
        assertTrue(easy.getEase() > good.getEase());
    }
}
