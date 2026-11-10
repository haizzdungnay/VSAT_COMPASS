package com.vsatcompass.api.service.impl;

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
import com.vsatcompass.api.service.AdminExamWindowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminExamWindowServiceImpl implements AdminExamWindowService {

    private final ExamWindowRepository examWindowRepository;
    private final ExamRepository examRepository;
    private final SubjectRepository subjectRepository;
    private final ExamSessionRepository examSessionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminExamWindowResponse> listWindows(Long examId, String status) {
        List<ExamWindow> windows = (examId != null)
                ? examWindowRepository.findByExamIdOrderByCreatedAtDesc(examId)
                : examWindowRepository.findAllByOrderByCreatedAtDesc();

        if (windows.isEmpty()) {
            return List.of();
        }

        List<Long> examIds = windows.stream().map(ExamWindow::getExamId).distinct().toList();
        Map<Long, Exam> examMap = examRepository.findAllById(examIds).stream()
                .collect(Collectors.toMap(Exam::getId, e -> e));

        List<Long> subjectIds = examMap.values().stream().map(Exam::getSubjectId).distinct().toList();
        Map<Long, Subject> subjectMap = subjectRepository.findAllById(subjectIds).stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));

        List<Long> windowIds = windows.stream().map(ExamWindow::getId).toList();
        List<WindowSessionCount> counts = examSessionRepository.countSessionsByWindowIds(windowIds);
        Map<Long, Map<SessionStatus, Long>> countMap = new HashMap<>();
        for (WindowSessionCount c : counts) {
            countMap.computeIfAbsent(c.getWindowId(), k -> new EnumMap<>(SessionStatus.class))
                    .put(c.getStatus(), c.getCount());
        }

        return windows.stream()
                .map(w -> {
                    Map<SessionStatus, Long> wc = countMap.getOrDefault(w.getId(), Collections.emptyMap());
                    long active = wc.getOrDefault(SessionStatus.IN_PROGRESS, 0L);
                    long submitted = wc.getOrDefault(SessionStatus.SUBMITTED, 0L);
                    return toResponseWithCounts(w, examMap.get(w.getExamId()), subjectMap, active, submitted);
                })
                .filter(resp -> status == null || status.isBlank() || "ALL".equalsIgnoreCase(status) || status.equalsIgnoreCase(resp.getStatus()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminExamWindowResponse getWindow(Long id) {
        ExamWindow window = examWindowRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("ExamWindow", id));
        Exam exam = examRepository.findById(window.getExamId()).orElse(null);
        Subject subject = (exam != null) ? subjectRepository.findById(exam.getSubjectId()).orElse(null) : null;
        Map<Long, Subject> subjectMap = (subject != null) ? Map.of(subject.getId(), subject) : Map.of();
        long active = examSessionRepository.countByExamWindowIdAndStatus(window.getId(), SessionStatus.IN_PROGRESS);
        long submitted = examSessionRepository.countByExamWindowIdAndStatus(window.getId(), SessionStatus.SUBMITTED);
        return toResponseWithCounts(window, exam, subjectMap, active, submitted);
    }

    @Override
    @Transactional
    public AdminExamWindowResponse createWindow(AdminExamWindowRequest req) {
        Exam exam = examRepository.findById(req.getExamId())
                .orElseThrow(() -> AppException.notFound("Exam", req.getExamId()));

        String windowCode = req.getWindowCode().trim();
        if (examWindowRepository.existsByExamIdAndWindowCode(req.getExamId(), windowCode)) {
            throw AppException.conflict("Mã ca thi '" + windowCode + "' đã tồn tại trong đề thi này");
        }

        ExamDeliveryMode effectiveMode = req.getMode() != null ? req.getMode() : ExamDeliveryMode.SCHEDULED_MOCK;
        validateTiming(effectiveMode, req.getStartAt(), req.getEndAt());

        ExamWindow window = ExamWindow.builder()
                .examId(req.getExamId())
                .windowCode(windowCode)
                .title(req.getTitle() != null && !req.getTitle().isBlank() ? req.getTitle().trim() : null)
                .mode(effectiveMode)
                .startAt(req.getStartAt())
                .endAt(req.getEndAt())
                .gracePeriodMinutes(req.getGracePeriodMinutes() != null ? req.getGracePeriodMinutes() : 0)
                .maxAttempts(req.getMaxAttempts())
                .resultPolicy(req.getResultPolicy() != null ? req.getResultPolicy() : ResultReleasePolicy.IMMEDIATE_FULL)
                .hardStopAtEnd(req.getHardStopAtEnd() != null ? req.getHardStopAtEnd() : true)
                .allowOffline(req.getAllowOffline() != null ? req.getAllowOffline() : false)
                .build();

        try {
            ExamWindow saved = examWindowRepository.save(window);
            log.info("Created exam window {} (id={}) for exam {}", windowCode, saved.getId(), req.getExamId());

            Subject subject = subjectRepository.findById(exam.getSubjectId()).orElse(null);
            Map<Long, Subject> subjectMap = (subject != null) ? Map.of(subject.getId(), subject) : Map.of();
            return toResponseWithCounts(saved, exam, subjectMap, 0L, 0L);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent duplicate window code insertion: {}", windowCode, ex);
            throw AppException.conflict("Mã ca thi '" + windowCode + "' đã tồn tại trong đề thi này");
        }
    }

    @Override
    @Transactional
    public AdminExamWindowResponse updateWindow(Long id, AdminExamWindowRequest req) {
        ExamWindow window = examWindowRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("ExamWindow", id));

        Exam exam = examRepository.findById(req.getExamId())
                .orElseThrow(() -> AppException.notFound("Exam", req.getExamId()));

        ExamDeliveryMode effectiveMode = req.getMode() != null ? req.getMode() : ExamDeliveryMode.SCHEDULED_MOCK;

        boolean hasSessions = examSessionRepository.existsByExamWindowId(id);
        if (hasSessions) {
            if (!Objects.equals(window.getExamId(), req.getExamId())) {
                throw AppException.badRequest("Không thể thay đổi đề thi của ca thi đã có thí sinh tham gia");
            }
            if (window.getMode() != effectiveMode) {
                throw AppException.badRequest("Không thể thay đổi chế độ của ca thi đã có thí sinh tham gia");
            }
        }

        String windowCode = req.getWindowCode().trim();
        if (examWindowRepository.existsByExamIdAndWindowCodeAndIdNot(req.getExamId(), windowCode, id)) {
            throw AppException.conflict("Mã ca thi '" + windowCode + "' đã tồn tại trong đề thi này");
        }

        validateTiming(effectiveMode, req.getStartAt(), req.getEndAt());

        window.setExamId(req.getExamId());
        window.setWindowCode(windowCode);
        window.setTitle(req.getTitle() != null && !req.getTitle().isBlank() ? req.getTitle().trim() : null);
        window.setMode(effectiveMode);
        window.setStartAt(req.getStartAt());
        window.setEndAt(req.getEndAt());
        window.setGracePeriodMinutes(req.getGracePeriodMinutes() != null ? req.getGracePeriodMinutes() : 0);
        window.setMaxAttempts(req.getMaxAttempts());
        window.setResultPolicy(req.getResultPolicy() != null ? req.getResultPolicy() : ResultReleasePolicy.IMMEDIATE_FULL);
        window.setHardStopAtEnd(req.getHardStopAtEnd() != null ? req.getHardStopAtEnd() : true);
        window.setAllowOffline(req.getAllowOffline() != null ? req.getAllowOffline() : false);

        try {
            ExamWindow updated = examWindowRepository.save(window);
            Subject subject = subjectRepository.findById(exam.getSubjectId()).orElse(null);
            Map<Long, Subject> subjectMap = (subject != null) ? Map.of(subject.getId(), subject) : Map.of();
            long active = examSessionRepository.countByExamWindowIdAndStatus(window.getId(), SessionStatus.IN_PROGRESS);
            long submitted = examSessionRepository.countByExamWindowIdAndStatus(window.getId(), SessionStatus.SUBMITTED);
            return toResponseWithCounts(updated, exam, subjectMap, active, submitted);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent duplicate window code on update: {}", windowCode, ex);
            throw AppException.conflict("Mã ca thi '" + windowCode + "' đã tồn tại trong đề thi này");
        }
    }

    @Override
    @Transactional
    public AdminExamWindowResponse updateStatus(Long id, String targetStatus) {
        ExamWindow window = examWindowRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("ExamWindow", id));

        if (window.getMode() == ExamDeliveryMode.PRACTICE) {
            throw AppException.badRequest("Phòng luyện tập tự do không áp dụng đóng/mở theo ca");
        }

        boolean hasSessions = examSessionRepository.existsByExamWindowId(id);
        OffsetDateTime now = OffsetDateTime.now();

        if ("CLOSED".equalsIgnoreCase(targetStatus)) {
            window.setEndAt(now);
            if (window.getStartAt() == null || window.getStartAt().isAfter(now)) {
                window.setStartAt(now.minusSeconds(1));
            }
        } else if ("RUNNING".equalsIgnoreCase(targetStatus)) {
            if (window.getStartAt() == null || window.getStartAt().isAfter(now)) {
                window.setStartAt(now);
            }
            if (window.getEndAt() == null || !window.getEndAt().isAfter(window.getStartAt())) {
                window.setEndAt(window.getStartAt().plusHours(2));
            }
        } else if ("UPCOMING".equalsIgnoreCase(targetStatus)) {
            if (hasSessions) {
                throw AppException.badRequest("Không thể chuyển ca thi về Sắp diễn ra khi đã có thí sinh tham gia");
            }
            window.setStartAt(now.plusHours(1));
            window.setEndAt(now.plusHours(3));
        } else {
            throw AppException.badRequest("Trạng thái không hợp lệ: " + targetStatus + ". Chọn UPCOMING, RUNNING, CLOSED");
        }

        ExamWindow updated = examWindowRepository.save(window);
        log.info("Updated status for exam window id={} to target={}", id, targetStatus);

        Exam exam = examRepository.findById(updated.getExamId()).orElse(null);
        Subject subject = (exam != null) ? subjectRepository.findById(exam.getSubjectId()).orElse(null) : null;
        Map<Long, Subject> subjectMap = (subject != null) ? Map.of(subject.getId(), subject) : Map.of();
        long active = examSessionRepository.countByExamWindowIdAndStatus(window.getId(), SessionStatus.IN_PROGRESS);
        long submitted = examSessionRepository.countByExamWindowIdAndStatus(window.getId(), SessionStatus.SUBMITTED);
        return toResponseWithCounts(updated, exam, subjectMap, active, submitted);
    }

    @Override
    @Transactional
    public void deleteWindow(Long id) {
        ExamWindow window = examWindowRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("ExamWindow", id));

        if (examSessionRepository.existsByExamWindowId(id)) {
            throw AppException.badRequest("Không thể xóa ca thi đã có thí sinh tham gia");
        }

        try {
            examWindowRepository.delete(window);
            log.info("Deleted exam window id={}", id);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Failed to delete exam window id={} due to FK constraints", id, ex);
            throw AppException.badRequest("Không thể xóa ca thi đã có dữ liệu liên quan");
        }
    }

    private void validateTiming(ExamDeliveryMode mode, OffsetDateTime startAt, OffsetDateTime endAt) {
        if (mode == ExamDeliveryMode.SCHEDULED_MOCK) {
            if (startAt == null || endAt == null) {
                throw AppException.badRequest("Ca thi có hẹn giờ yêu cầu thời gian bắt đầu (startAt) và kết thúc (endAt)");
            }
            if (!endAt.isAfter(startAt)) {
                throw AppException.badRequest("Thời gian kết thúc phải sau thời gian bắt đầu");
            }
        }
    }

    private AdminExamWindowResponse toResponseWithCounts(ExamWindow w, Exam exam, Map<Long, Subject> subjectMap, long activeCandidates, long totalSubmissions) {
        String examCode = (exam != null) ? exam.getExamCode() : "";
        String examTitle = (exam != null) ? exam.getTitle() : "";
        Long subjectId = (exam != null) ? exam.getSubjectId() : null;
        String subjectName = "";
        if (subjectId != null && subjectMap.containsKey(subjectId)) {
            subjectName = subjectMap.get(subjectId).getName();
        }

        String computedStatus;
        if (w.getMode() == ExamDeliveryMode.PRACTICE) {
            computedStatus = "RUNNING";
        } else {
            OffsetDateTime now = OffsetDateTime.now();
            if (w.getStartAt() != null && now.isBefore(w.getStartAt())) {
                computedStatus = "UPCOMING";
            } else if (w.getEndAt() != null && !w.getEndAt().isAfter(now)) {
                computedStatus = "CLOSED";
            } else {
                computedStatus = "RUNNING";
            }
        }

        String title = w.getTitle();
        String name = (title != null && !title.isBlank()) ? title : w.getWindowCode();

        return AdminExamWindowResponse.builder()
                .id(w.getId())
                .examId(w.getExamId())
                .examCode(examCode)
                .examTitle(examTitle)
                .subjectId(subjectId)
                .subjectName(subjectName)
                .windowCode(w.getWindowCode())
                .title(title)
                .name(name)
                .mode(w.getMode())
                .startAt(w.getStartAt())
                .endAt(w.getEndAt())
                .gracePeriodMinutes(w.getGracePeriodMinutes())
                .maxAttempts(w.getMaxAttempts())
                .resultPolicy(w.getResultPolicy())
                .hardStopAtEnd(w.getHardStopAtEnd())
                .allowOffline(w.getAllowOffline())
                .status(computedStatus)
                .activeCandidates(activeCandidates)
                .totalSubmissions(totalSubmissions)
                .createdAt(w.getCreatedAt())
                .build();
    }
}
