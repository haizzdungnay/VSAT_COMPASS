package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.request.CreateQuestionRequest;
import com.vsatcompass.api.dto.request.ReviewActionRequest;
import com.vsatcompass.api.dto.request.UpdateQuestionRequest;
import com.vsatcompass.api.dto.response.QuestionListItemResponse;
import com.vsatcompass.api.dto.response.QuestionOptionResponse;
import com.vsatcompass.api.dto.response.QuestionResponse;
import com.vsatcompass.api.dto.response.QuestionReviewResponse;
import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.QuestionOption;
import com.vsatcompass.api.entity.QuestionReview;
import com.vsatcompass.api.entity.Subtopic;
import com.vsatcompass.api.entity.Topic;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.entity.enums.QuestionType;
import com.vsatcompass.api.entity.enums.ReviewAction;
import com.vsatcompass.api.entity.enums.UserRole;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.QuestionOptionRepository;
import com.vsatcompass.api.repository.QuestionRepository;
import com.vsatcompass.api.repository.QuestionReviewRepository;
import com.vsatcompass.api.repository.SubjectRepository;
import com.vsatcompass.api.repository.SubtopicRepository;
import com.vsatcompass.api.repository.TopicRepository;
import com.vsatcompass.api.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuestionReviewRepository questionReviewRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final SubtopicRepository subtopicRepository;

    // ---------- CREATE ----------
    @Override
    @Transactional
    public QuestionResponse create(Long currentUserId, CreateQuestionRequest req) {
        validateSubjectTopicSubtopic(req.getSubjectId(), req.getTopicId(), req.getSubtopicId());
        validateOptionsForType(req.getQuestionType(),
                req.getOptions().stream().map(o -> o.getIsCorrect()).toList(),
                req.getOptions().size());

        Question q = Question.builder()
                .questionCode(generateQuestionCode(req.getTopicId()))
                .subjectId(req.getSubjectId())
                .topicId(req.getTopicId())
                .subtopicId(req.getSubtopicId())
                .difficulty(req.getDifficulty())
                .questionType(req.getQuestionType())
                .questionText(req.getQuestionText())
                .questionHtml(req.getQuestionHtml())
                .imageUrl(req.getImageUrl())
                .explanation(req.getExplanation())
                .explanationHtml(req.getExplanationHtml())
                .source(req.getSource())
                .tags(req.getTags())
                .status(QuestionStatus.DRAFT)
                .version(1)
                .createdBy(currentUserId)
                .build();

        Question saved = questionRepository.save(q);

        for (CreateQuestionRequest.OptionInput opt : req.getOptions()) {
            questionOptionRepository.save(QuestionOption.builder()
                    .questionId(saved.getId())
                    .optionLabel(opt.getOptionLabel())
                    .optionText(opt.getOptionText())
                    .optionHtml(opt.getOptionHtml())
                    .imageUrl(opt.getImageUrl())
                    .isCorrect(opt.getIsCorrect())
                    .displayOrder(opt.getDisplayOrder())
                    .build());
        }

        return toQuestionResponse(saved);
    }

    // ---------- GET ----------
    @Override
    public QuestionResponse getById(Long currentUserId, String currentUserRole, Long questionId) {
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> AppException.notFound("Question", questionId));
        enforceReadAccess(currentUserId, currentUserRole, q);
        return toQuestionResponse(q);
    }

    // ---------- UPDATE ----------
    @Override
    @Transactional
    public QuestionResponse update(Long currentUserId, String currentUserRole, Long questionId, UpdateQuestionRequest req) {
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> AppException.notFound("Question", questionId));

        enforceWriteAccess(currentUserId, currentUserRole, q);

        if (!canEdit(q.getStatus())) {
            throw new AppException(HttpStatus.CONFLICT, "INVALID_STATE",
                    "Cannot edit question in status " + q.getStatus()
                            + ". Only DRAFT or NEEDS_REVISION are editable.");
        }

        // FK validation when subject/topic/subtopic touched
        if (req.getSubjectId() != null || req.getTopicId() != null || req.getSubtopicId() != null) {
            Long newSubjectId = req.getSubjectId() != null ? req.getSubjectId() : q.getSubjectId();
            Long newTopicId = req.getTopicId() != null ? req.getTopicId() : q.getTopicId();
            Long newSubtopicId = req.getSubtopicId() != null ? req.getSubtopicId() : q.getSubtopicId();
            validateSubjectTopicSubtopic(newSubjectId, newTopicId, newSubtopicId);
            q.setSubjectId(newSubjectId);
            q.setTopicId(newTopicId);
            q.setSubtopicId(newSubtopicId);
        }

        if (req.getDifficulty() != null) q.setDifficulty(req.getDifficulty());
        boolean questionTypeChanged = req.getQuestionType() != null;
        if (questionTypeChanged) q.setQuestionType(req.getQuestionType());
        if (req.getQuestionText() != null) q.setQuestionText(req.getQuestionText());
        if (req.getQuestionHtml() != null) q.setQuestionHtml(req.getQuestionHtml());
        if (req.getImageUrl() != null) q.setImageUrl(req.getImageUrl());
        if (req.getExplanation() != null) q.setExplanation(req.getExplanation());
        if (req.getExplanationHtml() != null) q.setExplanationHtml(req.getExplanationHtml());
        if (req.getSource() != null) q.setSource(req.getSource());
        if (req.getTags() != null) q.setTags(req.getTags());

        if (req.getOptions() != null) {
            QuestionType effectiveType = q.getQuestionType();
            validateOptionsForType(effectiveType,
                    req.getOptions().stream().map(o -> o.getIsCorrect()).toList(),
                    req.getOptions().size());

            questionOptionRepository.deleteByQuestionId(questionId);
            for (UpdateQuestionRequest.OptionInput opt : req.getOptions()) {
                questionOptionRepository.save(QuestionOption.builder()
                        .questionId(questionId)
                        .optionLabel(opt.getOptionLabel())
                        .optionText(opt.getOptionText())
                        .optionHtml(opt.getOptionHtml())
                        .imageUrl(opt.getImageUrl())
                        .isCorrect(opt.getIsCorrect())
                        .displayOrder(opt.getDisplayOrder())
                        .build());
            }
        } else if (questionTypeChanged) {
            List<QuestionOption> existingOptions =
                    questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(questionId);
            validateOptionsForType(q.getQuestionType(),
                    existingOptions.stream().map(QuestionOption::getIsCorrect).toList(),
                    existingOptions.size());
        }

        q.setVersion(q.getVersion() + 1);
        Question saved = questionRepository.save(q);
        return toQuestionResponse(saved);
    }

    // ---------- LIST BY CREATOR ----------
    @Override
    public Page<QuestionListItemResponse> listByCreator(Long currentUserId, QuestionStatus statusFilter, Pageable pageable) {
        Page<Question> page = (statusFilter == null)
                ? questionRepository.findByCreatedByOrderByUpdatedAtDesc(currentUserId, pageable)
                : questionRepository.findByCreatedByAndStatusOrderByUpdatedAtDesc(currentUserId, statusFilter, pageable);
        return page.map(this::toQuestionListItemResponse);
    }

    // ---------- LIST BY STATUS ----------
    @Override
    public Page<QuestionListItemResponse> listByStatus(QuestionStatus status, Pageable pageable) {
        return questionRepository.findByStatusOrderByUpdatedAtDesc(status, pageable)
                .map(this::toQuestionListItemResponse);
    }

    // ---------- SUBMIT FOR REVIEW ----------
    @Override
    @Transactional
    public QuestionResponse submitForReview(Long currentUserId, Long questionId) {
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> AppException.notFound("Question", questionId));

        if (!q.getCreatedBy().equals(currentUserId)) {
            throw AppException.forbidden("Only the question owner can submit for review.");
        }
        if (!canSubmit(q.getStatus())) {
            throw new AppException(HttpStatus.CONFLICT, "INVALID_STATE",
                    "Cannot submit question in status " + q.getStatus()
                            + ". Only DRAFT or NEEDS_REVISION can be submitted.");
        }

        q.setStatus(QuestionStatus.PENDING_REVIEW);
        Question saved = questionRepository.save(q);
        return toQuestionResponse(saved);
    }

    // ---------- ADMIN: APPROVE / REQUEST_REVISION / REJECT ----------
    @Override
    @Transactional
    public QuestionResponse approve(Long currentUserId, Long questionId, ReviewActionRequest req) {
        return performReviewAction(currentUserId, questionId, req,
                ReviewAction.APPROVE, QuestionStatus.APPROVED, false);
    }

    @Override
    @Transactional
    public QuestionResponse requestRevision(Long currentUserId, Long questionId, ReviewActionRequest req) {
        return performReviewAction(currentUserId, questionId, req,
                ReviewAction.REQUEST_REVISION, QuestionStatus.NEEDS_REVISION, true);
    }

    @Override
    @Transactional
    public QuestionResponse reject(Long currentUserId, Long questionId, ReviewActionRequest req) {
        return performReviewAction(currentUserId, questionId, req,
                ReviewAction.REJECT, QuestionStatus.ARCHIVED, true);
    }

    private QuestionResponse performReviewAction(
            Long currentUserId, Long questionId, ReviewActionRequest req,
            ReviewAction action, QuestionStatus newStatus, boolean commentRequired) {

        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> AppException.notFound("Question", questionId));

        if (!canReview(q.getStatus())) {
            throw new AppException(HttpStatus.CONFLICT, "INVALID_STATE",
                    "Cannot " + action + " question in status " + q.getStatus()
                            + ". Only PENDING_REVIEW questions can be reviewed.");
        }

        String comment = req == null ? null : req.getComment();
        if (commentRequired && (comment == null || comment.isBlank())) {
            throw AppException.validationFailed("Comment is required for " + action + " action.");
        }

        questionReviewRepository.save(QuestionReview.builder()
                .questionId(questionId)
                .reviewerId(currentUserId)
                .action(action)
                .comment(comment)
                .versionReviewed(q.getVersion())
                .build());

        q.setStatus(newStatus);
        q.setReviewedBy(currentUserId);
        Question saved = questionRepository.save(q);
        return toQuestionResponse(saved);
    }

    // ---------- HELPERS ----------
    private void validateSubjectTopicSubtopic(Long subjectId, Long topicId, Long subtopicId) {
        if (!subjectRepository.existsById(subjectId)) {
            throw AppException.notFound("Subject", subjectId);
        }
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> AppException.notFound("Topic", topicId));
        if (!topic.getSubjectId().equals(subjectId)) {
            throw AppException.validationFailed(
                    "Topic " + topicId + " does not belong to subject " + subjectId);
        }
        if (subtopicId != null) {
            Subtopic subtopic = subtopicRepository.findById(subtopicId)
                    .orElseThrow(() -> AppException.notFound("Subtopic", subtopicId));
            if (!subtopic.getTopicId().equals(topicId)) {
                throw AppException.validationFailed(
                        "Subtopic " + subtopicId + " does not belong to topic " + topicId);
            }
        }
    }

    private void validateOptionsForType(QuestionType type, List<Boolean> isCorrectFlags, int totalCount) {
        long correctCount = isCorrectFlags.stream()
                .filter(b -> Boolean.TRUE.equals(b))
                .count();

        if (type == QuestionType.SINGLE_CHOICE || type == QuestionType.TRUE_FALSE) {
            if (correctCount != 1) {
                throw AppException.validationFailed(
                        type + " requires exactly 1 correct option, got " + correctCount);
            }
        } else if (type == QuestionType.MULTIPLE_CHOICE) {
            if (correctCount < 1) {
                throw AppException.validationFailed(
                        "MULTIPLE_CHOICE requires at least 1 correct option");
            }
        }
        if (type == QuestionType.TRUE_FALSE && totalCount != 2) {
            throw AppException.validationFailed(
                    "TRUE_FALSE requires exactly 2 options, got " + totalCount);
        }
    }

    private void enforceReadAccess(Long currentUserId, String role, Question q) {
        if (isAdminOrAbove(role)) return;
        if (!q.getCreatedBy().equals(currentUserId)) {
            throw AppException.forbidden("Cannot access question " + q.getId() + " — not the owner.");
        }
    }

    private void enforceWriteAccess(Long currentUserId, String role, Question q) {
        if (isAdminOrAbove(role)) return;
        if (!q.getCreatedBy().equals(currentUserId)) {
            throw AppException.forbidden("Cannot edit question " + q.getId() + " — not the owner.");
        }
    }

    private boolean isAdminOrAbove(String role) {
        return UserRole.CONTENT_ADMIN.name().equals(role) || UserRole.SUPER_ADMIN.name().equals(role);
    }

    private static boolean canEdit(QuestionStatus s) {
        return s == QuestionStatus.DRAFT || s == QuestionStatus.NEEDS_REVISION;
    }

    private static boolean canSubmit(QuestionStatus s) {
        return s == QuestionStatus.DRAFT || s == QuestionStatus.NEEDS_REVISION;
    }

    private static boolean canReview(QuestionStatus s) {
        return s == QuestionStatus.PENDING_REVIEW;
    }

    private String generateQuestionCode(Long topicId) {
        return "Q-T" + topicId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // ---------- MAPPERS ----------
    private QuestionResponse toQuestionResponse(Question q) {
        List<QuestionOption> options =
                questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(q.getId());
        List<QuestionReview> reviews =
                questionReviewRepository.findByQuestionIdOrderByCreatedAtDesc(q.getId());
        return QuestionResponse.builder()
                .id(q.getId())
                .questionCode(q.getQuestionCode())
                .subjectId(q.getSubjectId())
                .topicId(q.getTopicId())
                .subtopicId(q.getSubtopicId())
                .difficulty(q.getDifficulty())
                .questionType(q.getQuestionType())
                .questionText(q.getQuestionText())
                .questionHtml(q.getQuestionHtml())
                .imageUrl(q.getImageUrl())
                .explanation(q.getExplanation())
                .explanationHtml(q.getExplanationHtml())
                .source(q.getSource())
                .tags(q.getTags())
                .status(q.getStatus())
                .version(q.getVersion())
                .createdBy(q.getCreatedBy())
                .reviewedBy(q.getReviewedBy())
                .createdAt(q.getCreatedAt())
                .updatedAt(q.getUpdatedAt())
                .options(options.stream().map(this::toOptionResponse).toList())
                .reviewHistory(reviews.stream().map(this::toReviewResponse).toList())
                .build();
    }

    private QuestionListItemResponse toQuestionListItemResponse(Question q) {
        return QuestionListItemResponse.builder()
                .id(q.getId())
                .questionCode(q.getQuestionCode())
                .subjectId(q.getSubjectId())
                .topicId(q.getTopicId())
                .difficulty(q.getDifficulty())
                .questionType(q.getQuestionType())
                .status(q.getStatus())
                .version(q.getVersion())
                .createdBy(q.getCreatedBy())
                .updatedAt(q.getUpdatedAt())
                .build();
    }

    private QuestionOptionResponse toOptionResponse(QuestionOption o) {
        return QuestionOptionResponse.builder()
                .id(o.getId())
                .optionLabel(o.getOptionLabel())
                .optionText(o.getOptionText())
                .optionHtml(o.getOptionHtml())
                .imageUrl(o.getImageUrl())
                .isCorrect(o.getIsCorrect())
                .displayOrder(o.getDisplayOrder())
                .build();
    }

    private QuestionReviewResponse toReviewResponse(QuestionReview r) {
        return QuestionReviewResponse.builder()
                .id(r.getId())
                .reviewerId(r.getReviewerId())
                .action(r.getAction())
                .comment(r.getComment())
                .versionReviewed(r.getVersionReviewed())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
