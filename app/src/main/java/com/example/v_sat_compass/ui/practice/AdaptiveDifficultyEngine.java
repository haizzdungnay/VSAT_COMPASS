package com.example.v_sat_compass.ui.practice;

import com.example.v_sat_compass.data.model.enums.Difficulty;

public class AdaptiveDifficultyEngine {

    private Difficulty current = Difficulty.MEDIUM;
    private int correctStreak = 0;

    public Difficulty current() {
        return current;
    }

    public Difficulty recordAnswer(boolean correct) {
        if (correct) {
            correctStreak++;
            if (correctStreak >= 3) {
                current = increase(current);
                correctStreak = 0;
            }
        } else {
            current = decrease(current);
            correctStreak = 0;
        }
        return current;
    }

    private Difficulty increase(Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return Difficulty.MEDIUM;
            case MEDIUM:
                return Difficulty.HARD;
            case HARD:
            case VERY_HARD:
            default:
                return Difficulty.VERY_HARD;
        }
    }

    private Difficulty decrease(Difficulty difficulty) {
        switch (difficulty) {
            case VERY_HARD:
                return Difficulty.HARD;
            case HARD:
                return Difficulty.MEDIUM;
            case MEDIUM:
            case EASY:
            default:
                return Difficulty.EASY;
        }
    }
}
