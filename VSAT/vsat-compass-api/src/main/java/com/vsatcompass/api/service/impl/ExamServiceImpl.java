package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.response.ExamDetailResponse;
import com.vsatcompass.api.dto.response.ExamDownloadBundleResponse;
import com.vsatcompass.api.dto.response.QuestionGroupResponse;
import com.vsatcompass.api.dto.response.ExamSummaryResponse;
import com.vsatcompass.api.entity.Exam;
import com.vsatcompass.api.entity.ExamQuestion;
import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.QuestionGroup;
import com.vsatcompass.api.entity.QuestionMatchingChoice;
import com.vsatcompass.api.entity.QuestionOption;
import com.vsatcompass.api.entity.enums.ExamPricingType;
import com.vsatcompass.api.entity.enums.ExamStatus;
import com.vsatcompass.api.entity.enums.QuestionType;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.ExamQuestionRepository;
import com.vsatcompass.api.repository.ExamRepository;
import com.vsatcompass.api.repository.QuestionGroupRepository;
import com.vsatcompass.api.repository.QuestionMatchingChoiceRepository;
import com.vsatcompass.api.repository.QuestionOptionRepository;
import com.vsatcompass.api.repository.QuestionRepository;
import com.vsatcompass.api.service.ExamService;
import com.vsatcompass.api.util.VsatFormatRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuestionGroupRepository questionGroupRepository;
    private final QuestionMatchingChoiceRepository questionMatchingChoiceRepository;

    @Override
    @Cacheable(value = "publicExams", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    public Page<ExamSummaryResponse> listPublishedFreeExams(Pageable pageable) {
        return examRepository
                .findByStatusAndPricingType(ExamStatus.PUBLISHED, ExamPricingType.FREE, pageable)
                .map(this::toSummaryResponse);
    }

    @Override
    @Cacheable(value = "publicExamDetail", key = "#id")
    public ExamDetailResponse getPublicExam(Long id) {
        Exam exam = examRepository
                .findByIdAndStatusAndPricingType(id, ExamStatus.PUBLISHED, ExamPricingType.FREE)
                .orElseThrow(() -> AppException.notFound("Exam", id));
        return toDetailResponse(exam);
    }

    @Override
    @Cacheable(value = "publicExamBundle", key = "#id")
    public ExamDownloadBundleResponse getDownloadBundle(Long id) {
        Exam exam = examRepository
                .findByIdAndStatusAndPricingType(id, ExamStatus.PUBLISHED, ExamPricingType.FREE)
                .filter(this::isZeroPrice)
                .orElseThrow(() -> AppException.notFound("Exam", id));

        List<ExamQuestion> examQuestions = examQuestionRepository
                .findByExamIdOrderByQuestionOrderAscIdAsc(exam.getId());

        List<Question> rawQuestions = examQuestions.stream()
                .map(eq -> questionRepository.findById(eq.getQuestionId())
                        .orElseThrow(() -> AppException.notFound("Question", eq.getQuestionId())))
                .toList();

        Set<Long> groupIds = rawQuestions.stream()
                .map(Question::getQuestionGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, QuestionGroupResponse> groupMap = groupIds.isEmpty()
                ? Collections.emptyMap()
                : questionGroupRepository.findAllById(groupIds).stream()
                        .collect(Collectors.toMap(QuestionGroup::getId, this::toQuestionGroupResponse));

        List<ExamDownloadBundleResponse.QuestionBundleItem> questions = new ArrayList<>(examQuestions.size());
        for (int i = 0; i < examQuestions.size(); i++) {
            questions.add(toBundleQuestion(examQuestions.get(i), rawQuestions.get(i), groupMap));
        }

        return ExamDownloadBundleResponse.builder()
                .id(exam.getId())
                .examCode(exam.getExamCode())
                .title(exam.getTitle())
                .subjectId(exam.getSubjectId())
                .description(exam.getDescription())
                .questionCount(questions.size())
                .durationMinutes(exam.getDurationMinutes())
                .difficulty(exam.getDifficulty())
                .pricingType(exam.getPricingType())
                .tags(exam.getTags())
                .publishDate(exam.getPublishDate())
                .examVersion(exam.getVersion())
                .updatedAt(exam.getUpdatedAt())
                .questions(questions)
                .build();
    }

    private ExamSummaryResponse toSummaryResponse(Exam exam) {
        return ExamSummaryResponse.builder()
                .id(exam.getId())
                .examCode(exam.getExamCode())
                .title(exam.getTitle())
                .subjectId(exam.getSubjectId())
                .description(exam.getDescription())
                .questionCount(computedQuestionCount(exam))
                .durationMinutes(exam.getDurationMinutes())
                .difficulty(exam.getDifficulty())
                .pricingType(exam.getPricingType())
                .tags(exam.getTags())
                .build();
    }

    private ExamDetailResponse toDetailResponse(Exam exam) {
        return ExamDetailResponse.builder()
                .id(exam.getId())
                .examCode(exam.getExamCode())
                .title(exam.getTitle())
                .subjectId(exam.getSubjectId())
                .description(exam.getDescription())
                .questionCount(computedQuestionCount(exam))
                .durationMinutes(exam.getDurationMinutes())
                .difficulty(exam.getDifficulty())
                .pricingType(exam.getPricingType())
                .tags(exam.getTags())
                .publishDate(exam.getPublishDate())
                .build();
    }

    private Integer computedQuestionCount(Exam exam) {
        return Math.toIntExact(examQuestionRepository.countByExamId(exam.getId()));
    }


    private QuestionGroupResponse toQuestionGroupResponse(QuestionGroup g) {
        if (g == null) return null;
        return QuestionGroupResponse.builder()
                .id(g.getId())
                .code(g.getCode())
                .subjectId(g.getSubjectId())
                .title(g.getTitle())
                .passageText(g.getPassageText())
                .passageHtml(g.getPassageHtml())
                .imageUrl(g.getImageUrl())
                .audioUrl(g.getAudioUrl())
                .audioDurationSec(g.getAudioDurationSec())
                .maxPlays(g.getMaxPlays() != null ? g.getMaxPlays() : 2)
                .build();
    }
    private ExamDownloadBundleResponse.QuestionBundleItem toBundleQuestion(
            ExamQuestion examQuestion,
            Question question,
            Map<Long, QuestionGroupResponse> groupMap
    ) {
        List<QuestionOption> options = questionOptionRepository
                .findByQuestionIdOrderByDisplayOrderAscIdAsc(question.getId());
        QuestionType questionType = question.getQuestionType();
        BigDecimal maxRawScore = examQuestion.getMaxRawScore() != null
                ? examQuestion.getMaxRawScore()
                : VsatFormatRules.pointsFor(questionType);

        List<ExamDownloadBundleResponse.MatchingChoiceDto> matchingChoices = null;
        if (questionType == QuestionType.MATCHING) {
            matchingChoices = loadMatchingChoices(question.getId(), options);
        }

        QuestionGroupResponse group = question.getQuestionGroupId() != null
                ? groupMap.get(question.getQuestionGroupId())
                : null;
        return ExamDownloadBundleResponse.QuestionBundleItem.builder()
                .id(question.getId())
                .questionCode(question.getQuestionCode())
                .subjectId(question.getSubjectId())
                .topicId(question.getTopicId())
                .subtopicId(question.getSubtopicId())
                .questionGroupId(question.getQuestionGroupId())
                .questionGroup(group)
                .difficulty(question.getDifficulty())
                .questionType(questionType)
                .questionText(question.getQuestionText())
                .questionHtml(question.getQuestionHtml())
                .imageUrl(question.getImageUrl())
                .questionOrder(examQuestion.getQuestionOrder())
                .maxRawScore(maxRawScore)
                .options(options.stream().map(opt -> toBundleOption(opt, questionType)).toList())
                .matchingChoices(matchingChoices)
                .build();
    }

    private ExamDownloadBundleResponse.OptionBundleItem toBundleOption(
            QuestionOption option,
            QuestionType questionType
    ) {
        String optionText = VsatFormatRules.sanitizeOptionTextForStudent(option.getOptionText(), questionType);
        String optionHtml = VsatFormatRules.sanitizeOptionTextForStudent(option.getOptionHtml(), questionType);
        return ExamDownloadBundleResponse.OptionBundleItem.builder()
                .id(option.getId())
                .optionLabel(option.getOptionLabel())
                .optionText(optionText)
                .optionHtml(optionHtml)
                .imageUrl(option.getImageUrl())
                .displayOrder(option.getDisplayOrder())
                .build();
    }

    private List<ExamDownloadBundleResponse.MatchingChoiceDto> loadMatchingChoices(
            Long questionId,
            List<QuestionOption> options
    ) {
        if (questionMatchingChoiceRepository != null) {
            List<QuestionMatchingChoice> found = questionMatchingChoiceRepository
                    .findByQuestionIdOrderByDisplayOrderAsc(questionId);
            if (found != null && !found.isEmpty()) {
                return found.stream()
                        .map(choice -> ExamDownloadBundleResponse.MatchingChoiceDto.builder()
                                .key(choice.getChoiceKey())
                                .text(choice.getChoiceText())
                                .build())
                        .toList();
            }
        }
        Set<String> derivedKeys = new LinkedHashSet<>();
        for (QuestionOption option : options) {
            String target = VsatFormatRules.matchTarget(option.getOptionText());
            if (!target.isEmpty()) {
                derivedKeys.add(target);
            }
        }
        if (derivedKeys.isEmpty()) {
            return null;
        }
        return derivedKeys.stream()
                .sorted()
                .map(k -> ExamDownloadBundleResponse.MatchingChoiceDto.builder()
                        .key(k)
                        .text(k)
                        .build())
                .toList();
    }

    private boolean isZeroPrice(Exam exam) {
        return exam.getPrice() == null || BigDecimal.ZERO.compareTo(exam.getPrice()) == 0;
    }
}
