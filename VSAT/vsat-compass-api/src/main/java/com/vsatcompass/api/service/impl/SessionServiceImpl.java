package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.request.SessionRequest;
import com.vsatcompass.api.dto.response.SessionResponse;
import com.vsatcompass.api.entity.ExamSession;
import com.vsatcompass.api.entity.enums.SessionMode;
import com.vsatcompass.api.entity.enums.SessionStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.ExamSessionRepository;
import com.vsatcompass.api.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final ExamSessionRepository examSessionRepository;

    @Override
    @Transactional
    public SessionResponse.SessionInfo startSession(Long userId, SessionRequest.StartSession request) {
        // Resolve session mode (default MOCK_EXAM)
        SessionMode mode = SessionMode.MOCK_EXAM;
        if (request.getMode() != null) {
            try {
                mode = SessionMode.valueOf(request.getMode());
            } catch (IllegalArgumentException e) {
                throw AppException.badRequest("Mode không hợp lệ. Chọn: MOCK_EXAM, PRACTICE");
            }
        }

        // Default totalQuestions to 0 if not provided (client will update via client-submit)
        int totalQuestions = request.getTotalQuestions() != null ? request.getTotalQuestions() : 0;

        ExamSession session = ExamSession.builder()
                .userId(userId)
                .examId(request.getExamId())
                .mode(mode)
                .status(SessionStatus.IN_PROGRESS)
                .totalQuestions(totalQuestions)
                .deviceType("ANDROID")
                .build();

        session = examSessionRepository.save(session);

        log.info("Session {} started by user {} for exam {} (mode={})",
                session.getId(), userId, request.getExamId(), mode);

        return toSessionInfo(session);
    }

    @Override
    @Transactional
    public SessionResponse.SessionInfo clientSubmit(Long userId, Long sessionId, SessionRequest.ClientSubmit request) {
        // Find session — must exist
        ExamSession session = examSessionRepository.findById(sessionId)
                .orElseThrow(() -> AppException.notFound("ExamSession", sessionId));

        // Owner check — the session must belong to this user
        if (!session.getUserId().equals(userId)) {
            throw AppException.sessionForbidden();
        }

        // Anti-replay: session must be IN_PROGRESS
        if (session.getStatus() == SessionStatus.SUBMITTED) {
            throw AppException.sessionAlreadySubmitted();
        }
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw AppException.badRequest("Phiên thi không ở trạng thái IN_PROGRESS (hiện tại: " + session.getStatus() + ")");
        }

        // Validate correctCount ≤ totalQuestions
        if (request.getCorrectCount() > request.getTotalQuestions()) {
            throw AppException.validationFailed("correctCount không được lớn hơn totalQuestions");
        }

        // Update session with client-computed results
        BigDecimal score = BigDecimal.valueOf(request.getScore());
        BigDecimal scorePercentage = BigDecimal.valueOf(request.getScore());
        int wrongCount = request.getTotalQuestions() - request.getCorrectCount();

        session.setStatus(SessionStatus.SUBMITTED);
        session.setSubmittedAt(OffsetDateTime.now());
        session.setScore(score);
        session.setScorePercentage(scorePercentage);
        session.setCorrectCount(request.getCorrectCount());
        session.setTotalQuestions(request.getTotalQuestions());
        session.setAnsweredCount(request.getTotalQuestions()); // client-side: assume all answered
        session.setWrongCount(wrongCount);
        session.setSkippedCount(0);
        session.setTimeSpentSeconds(request.getTimeSpentSeconds());

        session = examSessionRepository.save(session);

        log.info("Session {} submitted by user {}: score={}, correct={}/{}",
                session.getId(), userId, request.getScore(),
                request.getCorrectCount(), request.getTotalQuestions());

        return toSessionInfo(session);
    }

    private SessionResponse.SessionInfo toSessionInfo(ExamSession session) {
        return SessionResponse.SessionInfo.builder()
                .id(session.getId())
                .examId(session.getExamId())
                .mode(session.getMode().name())
                .status(session.getStatus().name())
                .score(session.getScore())
                .scorePercentage(session.getScorePercentage())
                .correctCount(session.getCorrectCount())
                .totalQuestions(session.getTotalQuestions())
                .timeSpentSeconds(session.getTimeSpentSeconds())
                .startedAt(session.getStartedAt())
                .submittedAt(session.getSubmittedAt())
                .build();
    }
}
