package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.AdminExamWindowRequest;
import com.vsatcompass.api.dto.response.AdminExamWindowResponse;
import com.vsatcompass.api.entity.Exam;
import com.vsatcompass.api.entity.ExamWindow;
import com.vsatcompass.api.entity.Subject;
import com.vsatcompass.api.entity.enums.ExamDeliveryMode;
import com.vsatcompass.api.entity.enums.ResultReleasePolicy;
import com.vsatcompass.api.entity.enums.SessionStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.ExamRepository;
import com.vsatcompass.api.repository.ExamSessionRepository;
import com.vsatcompass.api.repository.ExamWindowRepository;
import com.vsatcompass.api.repository.SubjectRepository;
import com.vsatcompass.api.repository.WindowSessionCount;
import com.vsatcompass.api.service.impl.AdminExamWindowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminExamWindowServiceTest {

    @Mock
    private ExamWindowRepository examWindowRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private ExamSessionRepository examSessionRepository;

    @InjectMocks
    private AdminExamWindowServiceImpl adminExamWindowService;

    private Exam testExam;
    private Subject testSubject;
    private ExamWindow testWindow;

    @BeforeEach
    void setUp() {
        testSubject = Subject.builder()
                .id(1L)
                .code("MATH")
                .name("Toán học")
                .build();

        testExam = Exam.builder()
                .id(10L)
                .examCode("EXAM_MATH_01")
                .title("Đề thi Toán V-SAT")
                .subjectId(1L)
                .build();

        testWindow = ExamWindow.builder()
                .id(100L)
                .examId(10L)
                .windowCode("CA_TOAN_01")
                .title("Ca thi sáng")
                .mode(ExamDeliveryMode.SCHEDULED_MOCK)
                .startAt(OffsetDateTime.now().minusHours(1))
                .endAt(OffsetDateTime.now().plusHours(1))
                .gracePeriodMinutes(15)
                .maxAttempts(1)
                .resultPolicy(ResultReleasePolicy.IMMEDIATE_FULL)
                .hardStopAtEnd(true)
                .allowOffline(false)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("listWindows should map metadata, computed status, and batched session counts")
    void listWindows_Success() {
        WindowSessionCount c1 = mock(WindowSessionCount.class);
        when(c1.getWindowId()).thenReturn(100L);
        when(c1.getStatus()).thenReturn(SessionStatus.IN_PROGRESS);
        when(c1.getCount()).thenReturn(5L);

        WindowSessionCount c2 = mock(WindowSessionCount.class);
        when(c2.getWindowId()).thenReturn(100L);
        when(c2.getStatus()).thenReturn(SessionStatus.SUBMITTED);
        when(c2.getCount()).thenReturn(12L);

        when(examWindowRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(testWindow));
        when(examRepository.findAllById(List.of(10L))).thenReturn(List.of(testExam));
        when(subjectRepository.findAllById(List.of(1L))).thenReturn(List.of(testSubject));
        when(examSessionRepository.countSessionsByWindowIds(List.of(100L))).thenReturn(List.of(c1, c2));

        List<AdminExamWindowResponse> res = adminExamWindowService.listWindows(null, "ALL");

        assertThat(res).hasSize(1);
        AdminExamWindowResponse item = res.get(0);
        assertThat(item.getWindowCode()).isEqualTo("CA_TOAN_01");
        assertThat(item.getStatus()).isEqualTo("RUNNING");
        assertThat(item.getActiveCandidates()).isEqualTo(5L);
        assertThat(item.getTotalSubmissions()).isEqualTo(12L);
        assertThat(item.getSubjectName()).isEqualTo("Toán học");
    }

    @Test
    @DisplayName("createWindow should persist new window with valid timings")
    void createWindow_Success() {
        AdminExamWindowRequest req = AdminExamWindowRequest.builder()
                .examId(10L)
                .windowCode("CA_TOAN_02")
                .title("Ca thi chiều")
                .mode(ExamDeliveryMode.SCHEDULED_MOCK)
                .startAt(OffsetDateTime.now().plusHours(2))
                .endAt(OffsetDateTime.now().plusHours(4))
                .gracePeriodMinutes(10)
                .maxAttempts(2)
                .resultPolicy(ResultReleasePolicy.AFTER_WINDOW_FULL)
                .build();

        when(examRepository.findById(10L)).thenReturn(Optional.of(testExam));
        when(examWindowRepository.existsByExamIdAndWindowCode(10L, "CA_TOAN_02")).thenReturn(false);
        when(examWindowRepository.save(any(ExamWindow.class))).thenAnswer(inv -> {
            ExamWindow w = inv.getArgument(0);
            w.setId(200L);
            return w;
        });
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));

        AdminExamWindowResponse res = adminExamWindowService.createWindow(req);

        assertThat(res.getId()).isEqualTo(200L);
        assertThat(res.getWindowCode()).isEqualTo("CA_TOAN_02");
        assertThat(res.getStatus()).isEqualTo("UPCOMING");
        verify(examWindowRepository).save(any(ExamWindow.class));
    }

    @Test
    @DisplayName("createWindow with null mode should default to SCHEDULED_MOCK and validate timing")
    void createWindow_NullMode_DefaultsAndValidates() {
        AdminExamWindowRequest req = AdminExamWindowRequest.builder()
                .examId(10L)
                .windowCode("CA_TOAN_NULL_MODE")
                .mode(null)
                .startAt(null)
                .endAt(null)
                .build();

        when(examRepository.findById(10L)).thenReturn(Optional.of(testExam));
        when(examWindowRepository.existsByExamIdAndWindowCode(10L, "CA_TOAN_NULL_MODE")).thenReturn(false);

        assertThatThrownBy(() -> adminExamWindowService.createWindow(req))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("createWindow with duplicate code should throw 409 conflict")
    void createWindow_DuplicateCode_ThrowsConflict() {
        AdminExamWindowRequest req = AdminExamWindowRequest.builder()
                .examId(10L)
                .windowCode("CA_TOAN_01")
                .mode(ExamDeliveryMode.PRACTICE)
                .build();

        when(examRepository.findById(10L)).thenReturn(Optional.of(testExam));
        when(examWindowRepository.existsByExamIdAndWindowCode(10L, "CA_TOAN_01")).thenReturn(true);

        assertThatThrownBy(() -> adminExamWindowService.createWindow(req))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("createWindow with endAt before startAt should throw 400 bad request")
    void createWindow_InvalidTiming_ThrowsBadRequest() {
        AdminExamWindowRequest req = AdminExamWindowRequest.builder()
                .examId(10L)
                .windowCode("CA_TOAN_ERR")
                .mode(ExamDeliveryMode.SCHEDULED_MOCK)
                .startAt(OffsetDateTime.now().plusHours(5))
                .endAt(OffsetDateTime.now().plusHours(2))
                .build();

        when(examRepository.findById(10L)).thenReturn(Optional.of(testExam));
        when(examWindowRepository.existsByExamIdAndWindowCode(10L, "CA_TOAN_ERR")).thenReturn(false);

        assertThatThrownBy(() -> adminExamWindowService.createWindow(req))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("updateWindow when sessions exist cannot change examId or mode")
    void updateWindow_HasSessions_CannotChangeExamOrMode() {
        AdminExamWindowRequest req = AdminExamWindowRequest.builder()
                .examId(999L) // changed examId
                .windowCode("CA_TOAN_01")
                .mode(ExamDeliveryMode.SCHEDULED_MOCK)
                .startAt(OffsetDateTime.now().plusHours(1))
                .endAt(OffsetDateTime.now().plusHours(2))
                .build();

        when(examWindowRepository.findById(100L)).thenReturn(Optional.of(testWindow));
        when(examRepository.findById(999L)).thenReturn(Optional.of(testExam));
        when(examSessionRepository.existsByExamWindowId(100L)).thenReturn(true);

        assertThatThrownBy(() -> adminExamWindowService.updateWindow(100L, req))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("updateStatus CLOSED should adjust endAt to now without modifying startAt")
    void updateStatus_Closed_SetsEndAt() {
        OffsetDateTime originalStart = testWindow.getStartAt();
        when(examWindowRepository.findById(100L)).thenReturn(Optional.of(testWindow));
        when(examWindowRepository.save(any(ExamWindow.class))).thenAnswer(inv -> inv.getArgument(0));
        when(examRepository.findById(10L)).thenReturn(Optional.of(testExam));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));

        AdminExamWindowResponse res = adminExamWindowService.updateStatus(100L, "CLOSED");

        assertThat(res.getStatus()).isEqualTo("CLOSED");
        assertThat(testWindow.getEndAt()).isBeforeOrEqualTo(OffsetDateTime.now());
        assertThat(testWindow.getStartAt()).isEqualTo(originalStart);
    }

    @Test
    @DisplayName("updateStatus UPCOMING when sessions exist should throw 400 bad request")
    void updateStatus_Upcoming_HasSessions_ThrowsBadRequest() {
        when(examWindowRepository.findById(100L)).thenReturn(Optional.of(testWindow));
        when(examSessionRepository.existsByExamWindowId(100L)).thenReturn(true);

        assertThatThrownBy(() -> adminExamWindowService.updateStatus(100L, "UPCOMING"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("updateStatus on PRACTICE window should throw 400 bad request")
    void updateStatus_PracticeWindow_ThrowsBadRequest() {
        testWindow.setMode(ExamDeliveryMode.PRACTICE);
        when(examWindowRepository.findById(100L)).thenReturn(Optional.of(testWindow));

        assertThatThrownBy(() -> adminExamWindowService.updateStatus(100L, "CLOSED"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("deleteWindow when sessions exist should throw 400 bad request")
    void deleteWindow_HasSessions_ThrowsBadRequest() {
        when(examWindowRepository.findById(100L)).thenReturn(Optional.of(testWindow));
        when(examSessionRepository.existsByExamWindowId(100L)).thenReturn(true);

        assertThatThrownBy(() -> adminExamWindowService.deleteWindow(100L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(examWindowRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteWindow when no sessions exist should succeed")
    void deleteWindow_Success() {
        when(examWindowRepository.findById(100L)).thenReturn(Optional.of(testWindow));
        when(examSessionRepository.existsByExamWindowId(100L)).thenReturn(false);

        adminExamWindowService.deleteWindow(100L);

        verify(examWindowRepository).delete(testWindow);
    }
}
