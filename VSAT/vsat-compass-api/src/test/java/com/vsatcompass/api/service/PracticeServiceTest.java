package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.PracticeGradeRequest;
import com.vsatcompass.api.dto.response.PracticeGradeResponse;
import com.vsatcompass.api.dto.response.SessionQuestionContentResponse;
import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.QuestionOption;
import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.entity.enums.QuestionType;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.QuestionOptionRepository;
import com.vsatcompass.api.repository.QuestionRepository;
import com.vsatcompass.api.service.impl.PracticeServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PracticeService - F2 adaptive practice")
class PracticeServiceTest {

    @Mock QuestionRepository questionRepository;
    @Mock QuestionOptionRepository questionOptionRepository;
    @InjectMocks PracticeServiceImpl practiceService;

    @Test
    @DisplayName("getQuestions filters by subtopic and difficulty, strips answers")
    void getQuestions_filtersAndStripsAnswers() {
        Question question = question(1L, QuestionStatus.APPROVED, Difficulty.MEDIUM);
        when(questionRepository.findPracticeQuestions(eq(30L), eq(null), eq(Difficulty.MEDIUM), any(), eq(PageRequest.of(0, 5))))
                .thenReturn(List.of(question));
        when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(1L))
                .thenReturn(List.of(option(10L, false), option(11L, true)));

        List<SessionQuestionContentResponse> result = practiceService.getQuestions(30L, null, Difficulty.MEDIUM, 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getOptions()).extracting("id").containsExactly(10L, 11L);
        assertThat(SessionQuestionContentResponse.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("correctAnswer", "explanation", "isCorrect");
        verify(questionRepository).findPracticeQuestions(eq(30L), eq(null), eq(Difficulty.MEDIUM), any(), eq(PageRequest.of(0, 5)));
    }

    @Test
    @DisplayName("grade returns correct with explanation for answered question")
    void grade_correct_returnsExplanation() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question(1L, QuestionStatus.PUBLISHED, Difficulty.EASY)));
        when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(1L))
                .thenReturn(List.of(option(10L, false), option(11L, true)));

        PracticeGradeResponse result = practiceService.grade(new PracticeGradeRequest(1L, 11L));

        assertThat(result.isCorrect()).isTrue();
        assertThat(result.getCorrectOptionIds()).containsExactly(11L);
        assertThat(result.getExplanation()).isEqualTo("Because option 11 is correct");
    }

    @Test
    @DisplayName("grade returns wrong with correct option id")
    void grade_wrong_returnsCorrectOption() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question(1L, QuestionStatus.PUBLISHED, Difficulty.EASY)));
        when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(1L))
                .thenReturn(List.of(option(10L, false), option(11L, true)));

        PracticeGradeResponse result = practiceService.grade(new PracticeGradeRequest(1L, 10L));

        assertThat(result.isCorrect()).isFalse();
        assertThat(result.getCorrectOptionIds()).containsExactly(11L);
    }

    @Test
    @DisplayName("grade rejects draft questions as not found")
    void grade_draftQuestion_rejected() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question(1L, QuestionStatus.DRAFT, Difficulty.EASY)));

        assertThatThrownBy(() -> practiceService.grade(new PracticeGradeRequest(1L, 10L)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private Question question(Long id, QuestionStatus status, Difficulty difficulty) {
        return Question.builder()
                .id(id)
                .questionCode("Q" + id)
                .subjectId(10L)
                .topicId(20L)
                .subtopicId(30L)
                .difficulty(difficulty)
                .questionType(QuestionType.SINGLE_CHOICE)
                .questionText("Question " + id)
                .explanation("Because option 11 is correct")
                .status(status)
                .version(1)
                .createdBy(99L)
                .build();
    }

    private QuestionOption option(Long id, boolean correct) {
        return QuestionOption.builder()
                .id(id)
                .questionId(1L)
                .optionLabel(id == 10L ? "A" : "B")
                .optionText("Option " + id)
                .isCorrect(correct)
                .displayOrder(id == 10L ? 1 : 2)
                .build();
    }
}
