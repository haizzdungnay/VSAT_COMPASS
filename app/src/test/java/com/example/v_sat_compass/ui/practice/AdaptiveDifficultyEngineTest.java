package com.example.v_sat_compass.ui.practice;

import static org.junit.Assert.assertEquals;

import com.example.v_sat_compass.data.model.enums.Difficulty;

import org.junit.Test;

public class AdaptiveDifficultyEngineTest {

    @Test
    public void startsAtMedium() {
        AdaptiveDifficultyEngine engine = new AdaptiveDifficultyEngine();

        assertEquals(Difficulty.MEDIUM, engine.current());
    }

    @Test
    public void threeCorrectIncreasesDifficulty() {
        AdaptiveDifficultyEngine engine = new AdaptiveDifficultyEngine();

        engine.recordAnswer(true);
        engine.recordAnswer(true);
        Difficulty result = engine.recordAnswer(true);

        assertEquals(Difficulty.HARD, result);
    }

    @Test
    public void wrongDecreasesDifficultyAndResetsStreak() {
        AdaptiveDifficultyEngine engine = new AdaptiveDifficultyEngine();

        engine.recordAnswer(true);
        engine.recordAnswer(true);
        Difficulty result = engine.recordAnswer(false);

        assertEquals(Difficulty.EASY, result);
        engine.recordAnswer(true);
        engine.recordAnswer(true);
        assertEquals(Difficulty.MEDIUM, engine.recordAnswer(true));
    }
}
