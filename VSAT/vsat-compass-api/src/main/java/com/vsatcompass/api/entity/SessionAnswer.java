package com.vsatcompass.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "session_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    @Column(name = "selected_option_id")
    private Long selectedOptionId;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "is_bookmarked", nullable = false)
    @Builder.Default
    private Boolean isBookmarked = false;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    @Column(name = "answered_at")
    private OffsetDateTime answeredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_payload", columnDefinition = "jsonb")
    private Map<String, Object> answerPayload;

    @Column(name = "correct_subitems")
    private Integer correctSubitems;

    @Column(name = "total_subitems")
    private Integer totalSubitems;

    @Column(name = "raw_points_awarded", precision = 7, scale = 2)
    private BigDecimal rawPointsAwarded;
}
