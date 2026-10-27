package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.response.LeaderboardEntryResponse;
import com.vsatcompass.api.dto.response.LeaderboardMeResponse;
import com.vsatcompass.api.entity.User;
import com.vsatcompass.api.entity.enums.UserRole;
import com.vsatcompass.api.entity.enums.UserStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.ExamSessionRepository;
import com.vsatcompass.api.repository.UserRepository;
import com.vsatcompass.api.repository.UserScoreAggregate;
import com.vsatcompass.api.service.impl.LeaderboardServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaderboardService - F9 opt-in leaderboard")
class LeaderboardServiceTest {

    @Mock ExamSessionRepository examSessionRepository;
    @Mock UserRepository userRepository;

    @InjectMocks LeaderboardServiceImpl leaderboardService;

    @Test
    @DisplayName("getTop: chỉ user opt-in xuất hiện, sắp xếp theo tổng điểm giảm dần")
    void getTop_onlyOptIn_sortedByScoreDesc() {
        User alpha = user(1L, "Alpha", true);
        User beta  = user(2L, "Beta",  true);
        User gamma = user(3L, "Gamma", false); // opt-out -> must be hidden

        when(examSessionRepository.aggregateScoresByUser()).thenReturn(List.of(
                aggregate(1L, "900", 3),
                aggregate(2L, "750", 2),
                aggregate(3L, "1200", 5)
        ));
        when(userRepository.findAllById(any())).thenReturn(List.of(alpha, beta, gamma));

        List<LeaderboardEntryResponse> result = leaderboardService.getTop(20);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
        assertThat(result.get(0).getRank()).isEqualTo(1);
        assertThat(result.get(0).getTotalScore()).isEqualTo(900L);
        assertThat(result.get(0).getDisplayName()).isEqualTo("Alpha");
        assertThat(result.get(1).getUserId()).isEqualTo(2L);
        assertThat(result.get(1).getRank()).isEqualTo(2);
        assertThat(result.get(1).getTotalScore()).isEqualTo(750L);
        // Gamma (opt-out, score 1200) MUST NOT appear
        assertThat(result).extracting(LeaderboardEntryResponse::getUserId).doesNotContain(3L);
    }

    @Test
    @DisplayName("getTop: bằng điểm, ưu tiên user có nhiều bài hơn")
    void getTop_tieBreaker_isExamCount() {
        User a = user(1L, "Alice", true);
        User b = user(2L, "Bob",   true);

        when(examSessionRepository.aggregateScoresByUser()).thenReturn(List.of(
                aggregate(1L, "600", 2),
                aggregate(2L, "600", 4)
        ));
        when(userRepository.findAllById(any())).thenReturn(List.of(a, b));

        List<LeaderboardEntryResponse> result = leaderboardService.getTop(20);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserId()).isEqualTo(2L);
        assertThat(result.get(1).getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getTop: không có ai opt-in -> trả list rỗng (kể cả khi có score)")
    void getTop_noOptIn_returnsEmpty() {
        when(examSessionRepository.aggregateScoresByUser()).thenReturn(List.of(
                aggregate(1L, "500", 1)
        ));
        User optedOut = user(1L, "Solo", false);
        when(userRepository.findAllById(any())).thenReturn(List.of(optedOut));

        List<LeaderboardEntryResponse> result = leaderboardService.getTop(20);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getTop: chưa có session nào -> trả list rỗng")
    void getTop_noSubmissions_returnsEmpty() {
        when(examSessionRepository.aggregateScoresByUser()).thenReturn(List.of());
        // findAllById is never called when there are no aggregates
        List<LeaderboardEntryResponse> result = leaderboardService.getTop(20);
        assertThat(result).isEmpty();
        verify(userRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("getMe: opt-in true, có score -> trả về ownEntry")
    void getMe_optInWithScore_returnsOwnEntry() {
        User me = user(42L, "Me", true);
        when(userRepository.findById(42L)).thenReturn(Optional.of(me));
        when(examSessionRepository.aggregateScoresByUser()).thenReturn(List.of(
                aggregate(42L, "850", 2)
        ));

        LeaderboardMeResponse result = leaderboardService.getMe(42L);

        assertThat(result.isOptIn()).isTrue();
        assertThat(result.getOwnEntry()).isNotNull();
        assertThat(result.getOwnEntry().getUserId()).isEqualTo(42L);
        assertThat(result.getOwnEntry().getDisplayName()).isEqualTo("Me");
        assertThat(result.getOwnEntry().getTotalScore()).isEqualTo(850L);
    }

    @Test
    @DisplayName("getMe: opt-in false -> ownEntry=null")
    void getMe_optOut_returnsNoOwnEntry() {
        User me = user(42L, "Me", false);
        when(userRepository.findById(42L)).thenReturn(Optional.of(me));

        LeaderboardMeResponse result = leaderboardService.getMe(42L);

        assertThat(result.isOptIn()).isFalse();
        assertThat(result.getOwnEntry()).isNull();
        // The big aggregation is skipped for opt-out users.
        verify(examSessionRepository, never()).aggregateScoresByUser();
    }

    @Test
    @DisplayName("setOptIn: persist + trả về trạng thái mới")
    void setOptIn_persistsAndReturns() {
        User me = user(42L, "Me", false);
        when(userRepository.findById(42L)).thenReturn(Optional.of(me));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaderboardMeResponse result = leaderboardService.setOptIn(42L, true);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getLeaderboardOptIn()).isTrue();
        assertThat(result.isOptIn()).isTrue();
    }

    @Test
    @DisplayName("getMe: null userId -> 401")
    void getMe_nullUserId_throwsUnauthorized() {
        assertThatThrownBy(() -> leaderboardService.getMe(null))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("getMe: user không tồn tại -> 404")
    void getMe_missingUser_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaderboardService.getMe(99L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ae.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
                });
    }

    // ── helpers ──

    private User user(Long id, String fullName, boolean optIn) {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-18T00:00:00Z");
        return User.builder()
                .id(id)
                .email("u" + id + "@vsat.test")
                .passwordHash("x")
                .fullName(fullName)
                .role(UserRole.STUDENT)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .leaderboardOptIn(optIn)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private UserScoreAggregate aggregate(Long userId, String total, long examCount) {
        return new UserScoreAggregate() {
            @Override public Long getUserId() { return userId; }
            @Override public BigDecimal getTotalScore() { return new BigDecimal(total); }
            @Override public Long getExamCount() { return examCount; }
        };
    }
}
