package com.vsatcompass.api.specification;

import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.entity.enums.QuestionType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionSpecifications")
class QuestionSpecificationsTest {

    @Mock Root<Question> root;
    @Mock CriteriaQuery<?> query;
    @Mock CriteriaBuilder cb;
    @Mock Predicate predicate;

    @Test
    @DisplayName("hasStatus: null returns null")
    void hasStatus_null_returnsNull() {
        assertThat(QuestionSpecifications.hasStatus(null)).isNull();
    }

    @Test
    @DisplayName("hasStatus: non-null filters by status")
    void hasStatus_nonNull_filtersByStatus() {
        Path<QuestionStatus> path = path();
        when(root.<QuestionStatus>get("status")).thenReturn(path);
        when(cb.equal(path, QuestionStatus.APPROVED)).thenReturn(predicate);

        Predicate result = QuestionSpecifications.hasStatus(QuestionStatus.APPROVED)
                .toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
        verify(cb).equal(path, QuestionStatus.APPROVED);
    }

    @Test
    @DisplayName("hasSubject: null returns null")
    void hasSubject_null_returnsNull() {
        assertThat(QuestionSpecifications.hasSubject(null)).isNull();
    }

    @Test
    @DisplayName("hasSubject: non-null filters by subjectId")
    void hasSubject_nonNull_filtersBySubjectId() {
        Path<Long> path = path();
        when(root.<Long>get("subjectId")).thenReturn(path);
        when(cb.equal(path, 1L)).thenReturn(predicate);

        Predicate result = QuestionSpecifications.hasSubject(1L).toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
        verify(cb).equal(path, 1L);
    }

    @Test
    @DisplayName("hasTopic: null returns null")
    void hasTopic_null_returnsNull() {
        assertThat(QuestionSpecifications.hasTopic(null)).isNull();
    }

    @Test
    @DisplayName("hasTopic: non-null filters by topicId")
    void hasTopic_nonNull_filtersByTopicId() {
        Path<Long> path = path();
        when(root.<Long>get("topicId")).thenReturn(path);
        when(cb.equal(path, 10L)).thenReturn(predicate);

        Predicate result = QuestionSpecifications.hasTopic(10L).toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
        verify(cb).equal(path, 10L);
    }

    @Test
    @DisplayName("hasQuestionType: null returns null")
    void hasQuestionType_null_returnsNull() {
        assertThat(QuestionSpecifications.hasQuestionType(null)).isNull();
    }

    @Test
    @DisplayName("hasQuestionType: non-null filters by questionType")
    void hasQuestionType_nonNull_filtersByQuestionType() {
        Path<QuestionType> path = path();
        when(root.<QuestionType>get("questionType")).thenReturn(path);
        when(cb.equal(path, QuestionType.MULTIPLE_CHOICE)).thenReturn(predicate);

        Predicate result = QuestionSpecifications.hasQuestionType(QuestionType.MULTIPLE_CHOICE)
                .toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
        verify(cb).equal(path, QuestionType.MULTIPLE_CHOICE);
    }

    @Test
    @DisplayName("textContains: null returns null")
    void textContains_null_returnsNull() {
        assertThat(QuestionSpecifications.textContains(null)).isNull();
    }

    @Test
    @DisplayName("textContains: blank returns null")
    void textContains_blank_returnsNull() {
        assertThat(QuestionSpecifications.textContains("   ")).isNull();
    }

    @Test
    @DisplayName("textContains: valid keyword matches questionText")
    void textContains_validKeyword_matchesQuestionText() {
        TextContainsStubs stubs = stubTextContains("%linear%");

        Specification<Question> spec = QuestionSpecifications.textContains(" Linear ");
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(stubs.combinedPredicate);
        verify(cb).like(stubs.lowerQuestionText, "%linear%");
    }

    @Test
    @DisplayName("textContains: valid keyword matches questionCode")
    void textContains_validKeyword_matchesQuestionCode() {
        TextContainsStubs stubs = stubTextContains("%q-t10%");

        Specification<Question> spec = QuestionSpecifications.textContains(" Q-T10 ");
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(stubs.combinedPredicate);
        verify(cb).like(stubs.lowerQuestionCode, "%q-t10%");
    }

    private TextContainsStubs stubTextContains(String pattern) {
        Path<String> questionTextPath = path();
        Path<String> questionCodePath = path();
        Expression<String> lowerQuestionText = expression();
        Expression<String> lowerQuestionCode = expression();
        Predicate questionTextPredicate = mock(Predicate.class);
        Predicate questionCodePredicate = mock(Predicate.class);
        Predicate combinedPredicate = mock(Predicate.class);

        when(root.<String>get("questionText")).thenReturn(questionTextPath);
        when(root.<String>get("questionCode")).thenReturn(questionCodePath);
        when(cb.lower(questionTextPath)).thenReturn(lowerQuestionText);
        when(cb.lower(questionCodePath)).thenReturn(lowerQuestionCode);
        when(cb.like(lowerQuestionText, pattern)).thenReturn(questionTextPredicate);
        when(cb.like(lowerQuestionCode, pattern)).thenReturn(questionCodePredicate);
        when(cb.or(questionTextPredicate, questionCodePredicate)).thenReturn(combinedPredicate);

        return new TextContainsStubs(lowerQuestionText, lowerQuestionCode, combinedPredicate);
    }

    @SuppressWarnings("unchecked")
    private <T> Path<T> path() {
        return mock(Path.class);
    }

    @SuppressWarnings("unchecked")
    private <T> Expression<T> expression() {
        return mock(Expression.class);
    }

    private record TextContainsStubs(
            Expression<String> lowerQuestionText,
            Expression<String> lowerQuestionCode,
            Predicate combinedPredicate
    ) {
    }
}
