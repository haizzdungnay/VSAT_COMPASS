package com.vsatcompass.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * F9 — current user's leaderboard profile (visibility + the user's own row
 * when they have already opted in, or {@code ownEntry=null} otherwise).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardMeResponse {

    @JsonProperty("opt_in")
    private boolean optIn;

    /** Nullable — null if the user has opted in but has no submitted sessions yet. */
    @JsonProperty("own_entry")
    private LeaderboardEntryResponse ownEntry;
}
