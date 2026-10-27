package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.request.PracticeGradeRequest;
import com.vsatcompass.api.dto.response.PracticeGradeResponse;
import com.vsatcompass.api.dto.response.QuestionOptionContentResponse;
import com.vsatcompass.api.dto.response.SessionQuestionContentResponse;
import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.QuestionOption;
import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.QuestionOptionRepository;
import com.vsatcompass.api.repository.QuestionRepository;
import com.vsatcompass.api.service.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PracticeServiceImpl implements PracticeService {

    private static final List<QuestionStatus> VISIBLE_STATUSES = List.of(
            QuestionStatus.APPROVED,
            QuestionStatus.PUBLISHED
    );

    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SessionQuestionContentResponse> getQuestions(
            Long subtopicId,
            Long topicId,
            Difficulty difficulty,
            int limit
    ) {
        Difficulty effectiveDifficulty = difficulty != null ? difficulty : Difficulty.MEDIUM;
        int boundedLimit = Math.max(1, Math.min(limit, 50));
        return questionRepository
                .findPracticeQuestions(subtopicId, topicId, effectiveDifficulty, VISIBLE_STATUSES,
                        PageRequest.of(0, boundedLimit))
                .stream()
                .map(this::toQuestionContent)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PracticeGradeResponse grade(PracticeGradeRequest request) {
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> AppException.notFound("Question", request.getQuestionId()));
        if (!VISIBLE_STATUSES.contains(question.getStatus())) {
            throw AppException.notFound("Question", request.getQuestionId());
        }

        List<QuestionOption> options = questionOptionRepository
                .findByQuestionIdOrderByDisplayOrderAscIdAsc(question.getId());
        QuestionOption selected = options.stream()
                .filter(option -> Objects.equals(option.getId(), request.getSelectedOptionId()))
                .findFirst()
                .orElseThrow(() -> AppException.badRequest("Option khong thuoc cau hoi nay"));

        List<Long> correctOptionIds = options.stream()
                .filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                .map(QuestionOption::getId)
                .toList();

        return PracticeGradeResponse.builder()
                .questionId(question.getId())
                .selectedOptionId(selected.getId())
                .correct(Boolean.TRUE.equals(selected.getIsCorrect()))
                .correctOptionIds(correctOptionIds)
                .explanation(question.getExplanation())
                .build();
    }

    private SessionQuestionContentResponse toQuestionContent(Question question) {
        List<QuestionOptionContentResponse> options = questionOptionRepository
                .findByQuestionIdOrderByDisplayOrderAscIdAsc(question.getId())
                .stream()
                .map(option -> QuestionOptionContentResponse.builder()
                        .id(option.getId())
                        .content(option.getOptionText())
                        .order(option.getDisplayOrder())
                        .build())
                .toList();

        return SessionQuestionContentResponse.builder()
                .id(question.getId())
                .questionCode(question.getQuestionCode())
                .content(question.getQuestionText())
                .questionType(question.getQuestionType())
                .difficulty(question.getDifficulty())
                .order(0)
                .options(options)
                .build();
    }
}
