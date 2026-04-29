package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.SessionRequest;
import com.vsatcompass.api.dto.response.SessionResponse;
import com.vsatcompass.api.entity.ExamSession;
import com.vsatcompass.api.entity.enums.SessionMode;
import com.vsatcompass.api.entity.enums.SessionStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.ExamSessionRepository;
import com.vsatcompass.api.service.impl.SessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * Trust boundary note: SessionService.clientSubmit trusts the score computed by
 * the Android client (CLIENT_SIDE_EXAM_PROCESSING flag is true). These tests
 * verify the score is persisted as received, NOT re-derived server-side. That is
 * an architectural choice, not a bug.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService — characterization tests (Spring Boot 3.2.5 baseline)")
class SessionServiceTest {

    @Mock ExamSessionRepository examSessionRepository;

    @InjectMocks SessionServiceImpl sessionService;

    private static final Long USER_ID = 7L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long EXAM_ID = 100L;
    private static final Long SESSION_ID = 555L;

    private ExamSession inProgressSession;

    @BeforeEach
    void setUp() {
        inProgressSession = ExamSession.builder()
                .id(SESSION_ID)
                .userId(USER_ID)
                .examId(EXAM_ID)
                .mode(SessionMode.MOCK_EXAM)
                .status(SessionStatus.IN_PROGRESS)
                .totalQuestions(50)
                .answeredCount(0)
                .correctCount(0)
                .wrongCount(0)
                .skippedCount(0)
                .deviceType("ANDROID")
                .build();
    }

    private SessionRequest.StartSession startReq(Long examId, String mode, Integer totalQuestions) {
        SessionRequest.StartSession r = new SessionRequest.StartSession();
        r.setExamId(examId);
        r.setMode(mode);
        r.setTotalQuestions(totalQuestions);
        return r;
    }

    private SessionRequest.ClientSubmit submitReq(double score, int correctCount, int totalQuestions, int timeSpent) {
        SessionRequest.ClientSubmit r = new SessionRequest.ClientSubmit();
        r.setScore(score);
        r.setCorrectCount(correctCount);
        r.setTotalQuestions(totalQuestions);
        r.setTimeSpentSeconds(timeSpent);
        return r;
    }

    // ===== startSession =====

    @Test
    @DisplayName("startSession: happy path persists IN_PROGRESS session with given mode")
    void startSession_happyPath_persistsSession() {
        when(examSessionRepository.save(any(ExamSession.class))).thenAnswer(inv -> {
            ExamSession s = inv.getArgument(0);
            s.setId(SESSION_ID);
            return s;
        });

        SessionResponse.SessionInfo info = sessionService.startSession(
                USER_ID, startReq(EXAM_ID, "PRACTICE", 30));

        assertThat(info.getId()).isEqualTo(SESSION_ID);
        assertThat(info.getExamId()).isEqualTo(EXAM_ID);
        assertThat(info.getMode()).isEqualTo("PRACTICE");
        assertThat(info.getStatus()).isEqualTo("IN_PROGRESS");

        ArgumentCaptor<ExamSession> cap = ArgumentCaptor.forClass(ExamSession.class);
        verify(examSessionRepository).save(cap.capture());
        ExamSession saved = cap.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getMode()).isEqualTo(SessionMode.PRACTICE);
        assertThat(saved.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(saved.getTotalQuestions()).isEqualTo(30);
        assertThat(saved.getDeviceType()).isEqualTo("ANDROID");
    }

    @Test
    @DisplayName("startSession: invalid mode string throws BAD_REQUEST and does not save")
    void startSession_invalidMode_throwsBadRequest() {
        assertThatThrownBy(() -> sessionService.startSession(
                USER_ID, startReq(EXAM_ID, "NOT_A_MODE", 50)))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getCode()).isEqualTo("BAD_REQUEST");
                });

        verify(examSessionRepository, never()).save(any());
    }

    // ===== clientSubmit =====

    @Test
    @DisplayName("clientSubmit: happy path marks SUBMITTED and persists client-supplied score")
    void clientSubmit_happyPath_persistsClientScoreAndCounts() {
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));
        when(examSessionRepository.save(any(ExamSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse.SessionInfo info = sessionService.clientSubmit(
                USER_ID, SESSION_ID, submitReq(82.5, 41, 50, 1800));

        assertThat(info.getStatus()).isEqualTo("SUBMITTED");
        assertThat(info.getScore()).isEqualByComparingTo(BigDecimal.valueOf(82.5));
        assertThat(info.getCorrectCount()).isEqualTo(41);
        assertThat(info.getTotalQuestions()).isEqualTo(50);
        assertThat(info.getTimeSpentSeconds()).isEqualTo(1800);

        // Verify the saved entity reflects the client payload — server does NOT recompute
        ArgumentCaptor<ExamSession> cap = ArgumentCaptor.forClass(ExamSession.class);
        verify(examSessionRepository).save(cap.capture());
        ExamSession saved = cap.getValue();
        assertThat(saved.getStatus()).isEqualTo(SessionStatus.SUBMITTED);
        assertThat(saved.getSubmittedAt()).isNotNull();
        assertThat(saved.getScore()).isEqualByComparingTo(BigDecimal.valueOf(82.5));
        assertThat(saved.getScorePercentage()).isEqualByComparingTo(BigDecimal.valueOf(82.5));
        assertThat(saved.getCorrectCount()).isEqualTo(41);
        assertThat(saved.getWrongCount()).isEqualTo(9);
        assertThat(saved.getAnsweredCount()).isEqualTo(50);
        assertThat(saved.getSkippedCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("clientSubmit: unknown sessionId throws RESOURCE_NOT_FOUND with 404")
    void clientSubmit_unknownSessionId_throwsNotFound() {
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.clientSubmit(
                USER_ID, SESSION_ID, submitReq(80.0, 40, 50, 1800)))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
                });

        verify(examSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("clientSubmit: session belongs to a different user throws SESSION_FORBIDDEN with 403")
    void clientSubmit_differentUser_throwsForbidden() {
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));

        assertThatThrownBy(() -> sessionService.clientSubmit(
                OTHER_USER_ID, SESSION_ID, submitReq(80.0, 40, 50, 1800)))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ex.getCode()).isEqualTo("SESSION_FORBIDDEN");
                });

        verify(examSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("clientSubmit: already SUBMITTED session throws SESSION_ALREADY_SUBMITTED with 409 (anti-replay)")
    void clientSubmit_alreadySubmitted_throwsConflict() {
        inProgressSession.setStatus(SessionStatus.SUBMITTED);
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));

        assertThatThrownBy(() -> sessionService.clientSubmit(
                USER_ID, SESSION_ID, submitReq(80.0, 40, 50, 1800)))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getCode()).isEqualTo("SESSION_ALREADY_SUBMITTED");
                });

        verify(examSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("clientSubmit: session in non-IN_PROGRESS terminal state (e.g. ABANDONED) throws BAD_REQUEST")
    void clientSubmit_abandonedSession_throwsBadRequest() {
        inProgressSession.setStatus(SessionStatus.ABANDONED);
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));

        assertThatThrownBy(() -> sessionService.clientSubmit(
                USER_ID, SESSION_ID, submitReq(80.0, 40, 50, 1800)))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(examSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("clientSubmit: correctCount > totalQuestions throws VALIDATION_FAILED")
    void clientSubmit_correctGreaterThanTotal_throwsValidation() {
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));

        assertThatThrownBy(() -> sessionService.clientSubmit(
                USER_ID, SESSION_ID, submitReq(110.0, 60, 50, 1800)))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getCode()).isEqualTo("VALIDATION_FAILED");
                });

        verify(examSessionRepository, never()).save(any());
    }
}
