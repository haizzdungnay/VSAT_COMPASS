package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.response.ExamDetailResponse;
import com.vsatcompass.api.dto.response.ExamDownloadBundleResponse;
import com.vsatcompass.api.dto.response.ExamSummaryResponse;
import com.vsatcompass.api.entity.Exam;
import com.vsatcompass.api.entity.ExamQuestion;
import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.QuestionOption;
import com.vsatcompass.api.entity.enums.ExamPricingType;
import com.vsatcompass.api.entity.enums.ExamStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.ExamQuestionRepository;
import com.vsatcompass.api.repository.ExamRepository;
import com.vsatcompass.api.repository.QuestionOptionRepository;
import com.vsatcompass.api.repository.QuestionRepository;
import com.vsatcompass.api.service.ExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;

    @Override
    public Page<ExamSummaryResponse> listPublishedFreeExams(Pageable pageable) {
        return examRepository
                .findByStatusAndPricingType(ExamStatus.PUBLISHED, ExamPricingType.FREE, pageable)
                .map(this::toSummaryResponse);
    }

    @Override
    public ExamDetailResponse getPublicExam(Long id) {
        Exam exam = examRepository
                .findByIdAndStatusAndPricingType(id, ExamStatus.PUBLISHED, ExamPricingType.FREE)
                .orElseThrow(() -> AppException.notFound("Exam", id));
        return toDetailResponse(exam);
    }

    @Override
    public ExamDownloadBundleResponse getDownloadBundle(Long id) {
        Exam exam = examRepository
                .findByIdAndStatusAndPricingType(id, ExamStatus.PUBLISHED, ExamPricingType.FREE)
                .filter(this::isZeroPrice)
                .orElseThrow(() -> AppException.notFound("Exam", id));

        List<ExamDownloadBundleResponse.QuestionBundleItem> questions = examQuestionRepository
                .findByExamIdOrderByQuestionOrderAscIdAsc(exam.getId())
                .stream()
                .map(this::toBundleQuestion)
                .toList();

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

    private ExamDownloadBundleResponse.QuestionBundleItem toBundleQuestion(ExamQuestion examQuestion) {
        Question question = questionRepository.findById(examQuestion.getQuestionId())
                .orElseThrow(() -> AppException.notFound("Question", examQuestion.getQuestionId()));
        List<QuestionOption> options = questionOptionRepository
                .findByQuestionIdOrderByDisplayOrderAscIdAsc(question.getId());
        List<Long> correctOptionIds = options.stream()
                .filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                .map(QuestionOption::getId)
                .toList();
        String correctAnswer = options.stream()
                .filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                .map(QuestionOption::getOptionLabel)
                .findFirst()
                .orElse(null);

        return ExamDownloadBundleResponse.QuestionBundleItem.builder()
                .id(question.getId())
                .questionCode(question.getQuestionCode())
                .subjectId(question.getSubjectId())
                .topicId(question.getTopicId())
                .subtopicId(question.getSubtopicId())
                .difficulty(question.getDifficulty())
                .questionType(question.getQuestionType())
                .questionText(question.getQuestionText())
                .questionHtml(question.getQuestionHtml())
                .imageUrl(question.getImageUrl())
                .explanation(question.getExplanation())
                .explanationHtml(question.getExplanationHtml())
                .questionOrder(examQuestion.getQuestionOrder())
                .correctAnswer(correctAnswer)
                .correctOptionIds(correctOptionIds)
                .options(options.stream().map(this::toBundleOption).toList())
                .build();
    }

    private ExamDownloadBundleResponse.OptionBundleItem toBundleOption(QuestionOption option) {
        return ExamDownloadBundleResponse.OptionBundleItem.builder()
                .id(option.getId())
                .optionLabel(option.getOptionLabel())
                .optionText(option.getOptionText())
                .optionHtml(option.getOptionHtml())
                .imageUrl(option.getImageUrl())
                .isCorrect(option.getIsCorrect())
                .displayOrder(option.getDisplayOrder())
                .build();
    }

    private boolean isZeroPrice(Exam exam) {
        return exam.getPrice() == null || BigDecimal.ZERO.compareTo(exam.getPrice()) == 0;
    }
}
