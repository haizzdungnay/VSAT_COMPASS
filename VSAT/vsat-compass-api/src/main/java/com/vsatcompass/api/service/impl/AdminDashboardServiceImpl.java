package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.response.AdminStatsResponse;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.entity.enums.SessionStatus;
import com.vsatcompass.api.repository.ExamRepository;
import com.vsatcompass.api.repository.ExamSessionRepository;
import com.vsatcompass.api.repository.QuestionRepository;
import com.vsatcompass.api.repository.UserRepository;
import com.vsatcompass.api.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final QuestionRepository questionRepository;
    private final ExamSessionRepository examSessionRepository;
    private final UserRepository userRepository;
    private final ExamRepository examRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        ZoneOffset zone = ZoneOffset.UTC;
        LocalDate today = LocalDate.now(zone);
        OffsetDateTime todayStart = today.atStartOfDay().atOffset(zone);
        OffsetDateTime todayEnd = today.plusDays(1).atStartOfDay().atOffset(zone);

        int[] sessionsLast7Days = new int[7];
        for (int i = 0; i < 7; i++) {
            LocalDate day = today.minusDays(6 - i);
            OffsetDateTime dayStart = day.atStartOfDay().atOffset(zone);
            OffsetDateTime dayEnd = day.plusDays(1).atStartOfDay().atOffset(zone);
            sessionsLast7Days[i] = (int) examSessionRepository.countSubmittedOnDay(
                    SessionStatus.SUBMITTED, dayStart, dayEnd);
        }

        return AdminStatsResponse.builder()
                .pendingQuestions((int) questionRepository.countByStatus(QuestionStatus.PENDING_REVIEW))
                .revenueToday(0L)
                .errorTickets(0)
                .totalExams((int) examRepository.count())
                .totalUsers((int) userRepository.count())
                .totalSessionsToday((int) examSessionRepository.countByStatusAndSubmittedAtBetween(
                        SessionStatus.SUBMITTED, todayStart, todayEnd))
                .sessionsLast7Days(sessionsLast7Days)
                .build();
    }
}
