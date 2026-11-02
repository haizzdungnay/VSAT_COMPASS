package com.vsatcompass.api.entity;

import com.vsatcompass.api.entity.enums.ExamDeliveryMode;
import com.vsatcompass.api.entity.enums.ResultReleasePolicy;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "exam_windows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "window_code", nullable = false, length = 50)
    private String windowCode;

    @Column(name = "title", length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "mode", nullable = false, columnDefinition = "exam_delivery_mode")
    @Builder.Default
    private ExamDeliveryMode mode = ExamDeliveryMode.SCHEDULED_MOCK;

    @Column(name = "start_at")
    private OffsetDateTime startAt;

    @Column(name = "end_at")
    private OffsetDateTime endAt;

    @Column(name = "grace_period_minutes", nullable = false)
    @Builder.Default
    private Integer gracePeriodMinutes = 0;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "result_policy", nullable = false, columnDefinition = "result_release_policy")
    @Builder.Default
    private ResultReleasePolicy resultPolicy = ResultReleasePolicy.IMMEDIATE_FULL;

    @Column(name = "hard_stop_at_end", nullable = false)
    @Builder.Default
    private Boolean hardStopAtEnd = true;

    @Column(name = "allow_offline", nullable = false)
    @Builder.Default
    private Boolean allowOffline = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
