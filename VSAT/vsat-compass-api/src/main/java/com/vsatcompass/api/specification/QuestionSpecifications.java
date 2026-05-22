package com.vsatcompass.api.specification;

import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.entity.enums.QuestionType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class QuestionSpecifications {

    private QuestionSpecifications() {
    }

    public static Specification<Question> hasStatus(QuestionStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Question> hasSubject(Long subjectId) {
        if (subjectId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("subjectId"), subjectId);
    }

    public static Specification<Question> hasTopic(Long topicId) {
        if (topicId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("topicId"), topicId);
    }

    public static Specification<Question> hasQuestionType(QuestionType questionType) {
        if (questionType == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("questionType"), questionType);
    }

    public static Specification<Question> textContains(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String normalized = q.trim().toLowerCase(Locale.ROOT);
        String pattern = "%" + normalized + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("questionText")), pattern),
                cb.like(cb.lower(root.get("questionCode")), pattern)
        );
    }
}
