package com.example.v_sat_compass.data.model;

public class GamificationState {
    public static final int DAILY_CHALLENGE_TARGET_HINT = 10;
    private String lastActivityDate;
    private String challengeDate;
    private int currentStreak;
    private int totalXp;
    private int challengeProgress;
    private boolean challengeCompleted;

    public String getLastActivityDate() { return lastActivityDate; }
    public void setLastActivityDate(String lastActivityDate) { this.lastActivityDate = lastActivityDate; }
    public String getChallengeDate() { return challengeDate; }
    public void setChallengeDate(String challengeDate) { this.challengeDate = challengeDate; }
    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
    public int getTotalXp() { return totalXp; }
    public void setTotalXp(int totalXp) { this.totalXp = totalXp; }
    public int getChallengeProgress() { return challengeProgress; }
    public void setChallengeProgress(int challengeProgress) { this.challengeProgress = challengeProgress; }
    public boolean isChallengeCompleted() { return challengeCompleted; }
    public void setChallengeCompleted(boolean challengeCompleted) { this.challengeCompleted = challengeCompleted; }

    public int getLevel() {
        return Math.max(1, (totalXp / 500) + 1);
    }

    public int getXpIntoLevel() {
        return totalXp % 500;
    }

    public int getXpToNextLevel() {
        return 500 - getXpIntoLevel();
    }
}
