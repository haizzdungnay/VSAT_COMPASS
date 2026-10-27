package com.vsatcompass.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * F9 — body for {@code PATCH /me/leaderboard}. Lets the user flip the opt-in
 * flag. {@code optIn} is required so we never silently default to "off" — the
 * previous value is preserved when this is omitted, which is impossible here.
 */
@Data
public class LeaderboardOptInRequest {
    @NotNull(message = "optIn khong duoc de trong")
    private Boolean optIn;
}
