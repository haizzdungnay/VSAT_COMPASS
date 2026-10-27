package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.AiExplainRequest;
import com.vsatcompass.api.dto.response.AiExplainResponse;
import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.QuestionOption;
import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.entity.enums.QuestionType;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.QuestionOptionRepository;
import com.vsatcompass.api.repository.QuestionRepository;
import com.vsatcompass.api.service.impl.AiExplanationServiceImpl;
import com.vsatcompass.api.service.llm.LlmClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiExplanationService - F4 AI explain")
class AiExplanationServiceTest {

    @Mock QuestionRepository questionRepository;
    @Mock QuestionOptionRepository questionOptionRepository;
    @Mock LlmClient llmClient;
    @InjectMocks AiExplanationServiceImpl service;

    @Test
    @DisplayName("explain builds prompt, calls LLM, then caches by questionId")
    void explain_successThenCached() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question(QuestionStatus.PUBLISHED)));
        when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(1L))
                .thenReturn(List.of(option(10L, "A", false), option(11L, "B", true)));
        when(llmClient.complete(contains("Dap an dung"))).thenReturn("Giai thich them");

        AiExplainResponse first = service.explain(99L, new AiExplainRequest(1L, 10L));
        AiExplainResponse second = service.explain(99L, new AiExplainRequest(1L, 10L));

        assertThat(first.isCached()).isFalse();
        assertThat(second.isCached()).isTrue();
        assertThat(second.getExplanation()).isEqualTo("Giai thich them");
        verify(llmClient, times(1)).complete(anyString());
    }

    @Test
    @DisplayName("explain rejects hidden question as not found")
    void explain_hiddenRejected() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question(QuestionStatus.DRAFT)));

        assertThatThrownBy(() -> service.explain(99L, new AiExplainRequest(1L, null)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("explain propagates LLM configuration 503")
    void explain_llmNotConfigured_returns503() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question(QuestionStatus.PUBLISHED)));
        when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(1L))
                .thenReturn(List.of(option(10L, "A", false), option(11L, "B", true)));
        when(llmClient.complete(anyString()))
                .thenThrow(AppException.serviceUnavailable("LLM chua duoc cau hinh"));

        assertThatThrownBy(() -> service.explain(99L, new AiExplainRequest(1L, null)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    private Question question(QuestionStatus status) {
        return Question.builder()
                .id(1L)
                .questionCode("Q1")
                .subjectId(10L)
                .topicId(20L)
                .subtopicId(30L)
                .difficulty(Difficulty.MEDIUM)
                .questionType(QuestionType.SINGLE_CHOICE)
                .questionText("What is 2 + 2?")
                .explanation("Basic addition")
                .status(status)
                .version(1)
                .createdBy(99L)
                .build();
    }

    private QuestionOption option(Long id, String label, boolean correct) {
        return QuestionOption.builder()
                .id(id)
                .questionId(1L)
                .optionLabel(label)
                .optionText("Option " + label)
                .isCorrect(correct)
                .displayOrder(correct ? 2 : 1)
                .build();
    }
}
