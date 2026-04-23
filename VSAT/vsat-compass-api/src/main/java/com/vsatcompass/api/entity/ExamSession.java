package com.vsatcompass.api.entity;

import com.vsatcompass.api.entity.enums.SessionMode;
import com.vsatcompass.api.entity.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "exam_sessions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ExamSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "session_mode")
    @Builder.Default
    private SessionMode mode = SessionMode.MOCK_EXAM;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "session_status")
    @Builder.Default
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private OffsetDateTime startedAt = OffsetDateTime.now();

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "answered_count", nullable = false)
    @Builder.Default
    private Integer answeredCount = 0;

    @Column(name = "correct_count", nullable = false)
    @Builder.Default
    private Integer correctCount = 0;

    @Column(name = "wrong_count", nullable = false)
    @Builder.Default
    private Integer wrongCount = 0;

    @Column(name = "skipped_count", nullable = false)
    @Builder.Default
    private Integer skippedCount = 0;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "score_percentage", precision = 5, scale = 2)
    private BigDecimal scorePercentage;

    @Column(name = "device_type", length = 20)
    @Builder.Default
    private String deviceType = "ANDROID";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
