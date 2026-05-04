package com.vsatcompass.api.entity;

import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.entity.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_code", nullable = false, unique = true, length = 50)
    private String questionCode;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "subtopic_id")
    private Long subtopicId;

    @Column(name = "question_group_id")
    private Long questionGroupId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "difficulty", nullable = false, columnDefinition = "difficulty_level")
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "question_type", nullable = false, columnDefinition = "question_type")
    private QuestionType questionType;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "question_html", columnDefinition = "TEXT")
    private String questionHtml;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "explanation_html", columnDefinition = "TEXT")
    private String explanationHtml;

    @Column(name = "source", length = 255)
    private String source;

    @Column(name = "tags", length = 500)
    private String tags;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "question_status")
    private QuestionStatus status;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "order_in_group")
    private Integer orderInGroup;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
