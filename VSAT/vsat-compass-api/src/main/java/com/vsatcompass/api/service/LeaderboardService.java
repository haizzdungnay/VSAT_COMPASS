package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.response.LeaderboardEntryResponse;
import com.vsatcompass.api.dto.response.LeaderboardMeResponse;

import java.util.List;

public interface LeaderboardService {

    /** Public leaderboard: top-N opt-in users, ordered by total score desc. */
    List<LeaderboardEntryResponse> getTop(int limit);

    /** Current user's opt-in state + their own row (if they have any score). */
    LeaderboardMeResponse getMe(Long userId);

    /** Flip the opt-in flag for the current user. Returns the new state. */
    LeaderboardMeResponse setOptIn(Long userId, boolean optIn);
}
