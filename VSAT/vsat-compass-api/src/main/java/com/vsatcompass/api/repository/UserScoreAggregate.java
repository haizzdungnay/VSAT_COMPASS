package com.vsatcompass.api.repository;

import java.math.BigDecimal;

/**
 * F9 — projection for the per-user leaderboard aggregate. Spring Data binds
 * the JPQL aliases onto these getters.
 */
public interface UserScoreAggregate {
    Long getUserId();
    BigDecimal getTotalScore();
    Long getExamCount();
}
