package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.response.AdminStatsResponse;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.entity.enums.SessionStatus;
import com.vsatcompass.api.repository.ExamRepository;
import com.vsatcompass.api.repository.ExamSessionRepository;
import com.vsatcompass.api.repository.QuestionRepository;
import com.vsatcompass.api.repository.UserRepository;
import com.vsatcompass.api.service.impl.AdminDashboardServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock QuestionRepository questionRepository;
    @Mock ExamSessionRepository examSessionRepository;
    @Mock UserRepository userRepository;
    @Mock ExamRepository examRepository;

    @InjectMocks AdminDashboardServiceImpl adminDashboardService;

    @Test
    @DisplayName("getStats aggregates pending questions and session counts")
    void getStats_returnsAggregatedValues() {
        when(questionRepository.countByStatus(QuestionStatus.PENDING_REVIEW)).thenReturn(5L);
        when(examRepository.count()).thenReturn(12L);
        when(userRepository.count()).thenReturn(100L);
        when(examSessionRepository.countByStatusAndSubmittedAtBetween(
                eq(SessionStatus.SUBMITTED), any(), any())).thenReturn(3L);
        when(examSessionRepository.countSubmittedOnDay(
                eq(SessionStatus.SUBMITTED), any(), any())).thenReturn(1L, 2L, 0L, 4L, 3L, 2L, 5L);

        AdminStatsResponse stats = adminDashboardService.getStats();

        assertThat(stats.getPendingQuestions()).isEqualTo(5);
        assertThat(stats.getTotalExams()).isEqualTo(12);
        assertThat(stats.getTotalUsers()).isEqualTo(100);
        assertThat(stats.getTotalSessionsToday()).isEqualTo(3);
        assertThat(stats.getRevenueToday()).isZero();
        assertThat(stats.getErrorTickets()).isZero();
        assertThat(stats.getSessionsLast7Days()).hasSize(7);
        assertThat(stats.getSessionsLast7Days()[6]).isEqualTo(5);
    }
}
