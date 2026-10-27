package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.response.LeaderboardEntryResponse;
import com.vsatcompass.api.dto.response.LeaderboardMeResponse;
import com.vsatcompass.api.entity.User;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.ExamSessionRepository;
import com.vsatcompass.api.repository.UserRepository;
import com.vsatcompass.api.repository.UserScoreAggregate;
import com.vsatcompass.api.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaderboardServiceImpl implements LeaderboardService {

    private final ExamSessionRepository examSessionRepository;
    private final UserRepository userRepository;

    @Override
    public List<LeaderboardEntryResponse> getTop(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 100));

        Map<Long, UserScoreAggregate> aggregatesByUser = examSessionRepository
                .aggregateScoresByUser()
                .stream()
                .collect(Collectors.toMap(
                        UserScoreAggregate::getUserId,
                        Function.identity(),
                        (a, b) -> a
                ));

        if (aggregatesByUser.isEmpty()) {
            return List.of();
        }

        List<User> optInUsers = userRepository.findAllById(aggregatesByUser.keySet())
                .stream()
                .filter(u -> Boolean.TRUE.equals(u.getLeaderboardOptIn()))
                .toList();

        List<LeaderboardEntryResponse> ranked = optInUsers.stream()
                .map(user -> toEntry(user, aggregatesByUser.get(user.getId())))
                .filter(Objects::nonNull)
                .sorted(buildRankComparator())
                .limit(boundedLimit)
                .toList();

        // Stamp the 1-based rank on the limited list.
        List<LeaderboardEntryResponse> withRank = new java.util.ArrayList<>(ranked.size());
        for (int i = 0; i < ranked.size(); i++) {
            LeaderboardEntryResponse e = ranked.get(i);
            withRank.add(LeaderboardEntryResponse.builder()
                    .rank(i + 1)
                    .userId(e.getUserId())
                    .displayName(e.getDisplayName())
                    .avatarUrl(e.getAvatarUrl())
                    .totalScore(e.getTotalScore())
                    .examCount(e.getExamCount())
                    .build());
        }
        return withRank;
    }

    @Override
    public LeaderboardMeResponse getMe(Long userId) {
        User user = requireUser(userId);
        boolean optedIn = Boolean.TRUE.equals(user.getLeaderboardOptIn());
        LeaderboardEntryResponse ownEntry = null;
        if (optedIn) {
            ownEntry = examSessionRepository.aggregateScoresByUser().stream()
                    .filter(agg -> userId.equals(agg.getUserId()))
                    .findFirst()
                    .map(agg -> toEntry(user, agg))
                    .orElse(null);
        }
        return LeaderboardMeResponse.builder()
                .optIn(optedIn)
                .ownEntry(ownEntry)
                .build();
    }

    @Override
    @Transactional
    public LeaderboardMeResponse setOptIn(Long userId, boolean optIn) {
        User user = requireUser(userId);
        user.setLeaderboardOptIn(optIn);
        userRepository.save(user);
        return getMe(userId);
    }

    private User requireUser(Long userId) {
        if (userId == null) {
            throw AppException.authUnauthorized();
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User", userId));
    }

    private LeaderboardEntryResponse toEntry(User user, UserScoreAggregate aggregate) {
        if (user == null || aggregate == null) return null;
        BigDecimal total = aggregate.getTotalScore();
        long totalScore = total != null ? total.longValue() : 0L;
        long examCount = aggregate.getExamCount() != null ? aggregate.getExamCount() : 0L;
        return LeaderboardEntryResponse.builder()
                .userId(user.getId())
                .displayName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .totalScore(totalScore)
                .examCount(examCount)
                .build();
    }

    private Comparator<LeaderboardEntryResponse> buildRankComparator() {
        return (a, b) -> {
            int byScore = Long.compare(b.getTotalScore(), a.getTotalScore());     // desc
            if (byScore != 0) return byScore;
            int byCount = Long.compare(b.getExamCount(), a.getExamCount());       // desc
            if (byCount != 0) return byCount;
            String an = a.getDisplayName() == null ? "" : a.getDisplayName();
            String bn = b.getDisplayName() == null ? "" : b.getDisplayName();
            return an.compareToIgnoreCase(bn);                                    // asc
        };
    }
}
