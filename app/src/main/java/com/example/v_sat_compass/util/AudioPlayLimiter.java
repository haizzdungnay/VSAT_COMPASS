package com.example.v_sat_compass.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller and limiter for English Listening audio tracks during exams.
 * - Restricts plays per question group (default 2 plays).
 * - Disallows seeking/skipping forward/backward.
 */
public class AudioPlayLimiter {

    public static final int DEFAULT_MAX_PLAYS = 2;

    private final int defaultMaxPlays;
    private final Map<Long, Integer> playCounts = new HashMap<>();
    private final Map<Long, Integer> customMaxPlays = new HashMap<>();

    public AudioPlayLimiter() {
        this(DEFAULT_MAX_PLAYS);
    }

    public AudioPlayLimiter(int defaultMaxPlays) {
        this.defaultMaxPlays = defaultMaxPlays > 0 ? defaultMaxPlays : DEFAULT_MAX_PLAYS;
    }

    public void setGroupMaxPlays(long questionGroupId, int maxPlays) {
        if (maxPlays > 0) {
            customMaxPlays.put(questionGroupId, maxPlays);
        }
    }

    public int getMaxPlays(long questionGroupId) {
        Integer max = customMaxPlays.get(questionGroupId);
        return max != null ? max : defaultMaxPlays;
    }

    public int getPlayCount(long questionGroupId) {
        Integer count = playCounts.get(questionGroupId);
        return count != null ? count : 0;
    }

    public int getRemainingPlays(long questionGroupId) {
        int max = getMaxPlays(questionGroupId);
        int played = getPlayCount(questionGroupId);
        return Math.max(0, max - played);
    }

    public boolean canPlay(long questionGroupId) {
        return getRemainingPlays(questionGroupId) > 0;
    }

    public boolean recordPlay(long questionGroupId) {
        if (!canPlay(questionGroupId)) {
            return false;
        }
        int played = getPlayCount(questionGroupId);
        playCounts.put(questionGroupId, played + 1);
        return true;
    }

    /**
     * Standardized listening exams disallow seeking/scrubbing to prevent skipping or repeated review of specific words.
     * Returns false to reject arbitrary seek operations.
     */
    public boolean validateSeekPosition(long currentPositionMs, long targetPositionMs) {
        // Disallow seeking: only contiguous playback allowed
        return false;
    }

    public void reset() {
        playCounts.clear();
        customMaxPlays.clear();
    }
}
