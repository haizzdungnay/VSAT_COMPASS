package com.vsatcompass.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

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
}
