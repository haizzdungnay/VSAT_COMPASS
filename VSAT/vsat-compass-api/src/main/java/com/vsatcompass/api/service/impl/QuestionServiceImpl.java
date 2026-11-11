package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.request.CreateQuestionRequest;
import com.vsatcompass.api.dto.request.ReviewActionRequest;
import com.vsatcompass.api.dto.request.UpdateQuestionRequest;
import com.vsatcompass.api.dto.response.QuestionGroupResponse;
import com.vsatcompass.api.dto.response.QuestionListItemResponse;
import com.vsatcompass.api.dto.response.QuestionOptionResponse;
import com.vsatcompass.api.dto.response.QuestionPickerItemResponse;
import com.vsatcompass.api.dto.response.QuestionResponse;
import com.vsatcompass.api.dto.response.QuestionReviewResponse;
import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.QuestionGroup;
import com.vsatcompass.api.entity.QuestionOption;
import com.vsatcompass.api.entity.QuestionReview;
import com.vsatcompass.api.entity.Subtopic;
import com.vsatcompass.api.entity.Topic;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.entity.enums.QuestionType;
import com.vsatcompass.api.entity.enums.ReviewAction;
import com.vsatcompass.api.entity.enums.UserRole;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.QuestionGroupRepository;
import com.vsatcompass.api.repository.QuestionOptionRepository;
import com.vsatcompass.api.repository.QuestionRepository;
import com.vsatcompass.api.repository.QuestionReviewRepository;
import com.vsatcompass.api.repository.SubjectRepository;
import com.vsatcompass.api.repository.SubtopicRepository;
import com.vsatcompass.api.repository.TopicRepository;
import com.vsatcompass.api.service.QuestionService;
import com.vsatcompass.api.specification.QuestionSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
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
    private final QuestionGroupRepository questionGroupRepository;

    // ---------- CREATE ----------
    @Override
    @Transactional
    public QuestionResponse create(Long currentUserId, CreateQuestionRequest req) {
        validateSubjectTopicSubtopic(req.getSubjectId(), req.getTopicId(), req.getSubtopicId());
        QuestionGroup resolvedGroup = null;
        Long questionGroupId = req.getQuestionGroupId();
        if (questionGroupId != null) {
            resolvedGroup = questionGroupRepository.findById(questionGroupId)
                    .orElseThrow(() -> AppException.notFound("QuestionGroup", req.getQuestionGroupId()));
            if (!req.getSubjectId().equals(resolvedGroup.getSubjectId())) {
                throw AppException.badRequest("Question group subject must match question subject");
            }
        }
        validateOptionsForType(req.getQuestionType(),
                req.getOptions().stream().map(CreateQuestionRequest.OptionInput::getIsCorrect).toList(),
                req.getOptions().size());

        String resolvedAudioUrl = req.resolveAudioUrl();
        if (resolvedGroup != null) {
            resolvedGroup = updateGroupAudioFields(resolvedGroup, resolvedAudioUrl,
                    req.resolveAudioDurationSec(), req.resolveMaxPlays());
        } else if (resolvedAudioUrl != null && !resolvedAudioUrl.isBlank()) {
            resolvedGroup = createAudioQuestionGroup(req.getSubjectId(), resolvedAudioUrl,
                    req.resolveAudioDurationSec(), req.resolveMaxPlays());
            questionGroupId = resolvedGroup != null ? resolvedGroup.getId() : null;
        }

        Question q = Question.builder()
                .questionCode(generateQuestionCode(req.getTopicId()))
                .subjectId(req.getSubjectId())
                .topicId(req.getTopicId())
                .subtopicId(req.getSubtopicId())
                .questionGroupId(questionGroupId)
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

        return toQuestionResponse(saved, resolvedGroup);
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

        if (req.getLockVersion() != null && !req.getLockVersion().equals(q.getLockVersion())) {
            throw new org.springframework.orm.ObjectOptimisticLockingFailureException(Question.class, q.getId());
        }

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

        Long effectiveGroupId = req.getQuestionGroupId() != null ? req.getQuestionGroupId() : q.getQuestionGroupId();
        QuestionGroup resolvedGroup = null;
        if (effectiveGroupId != null) {
            resolvedGroup = questionGroupRepository.findById(effectiveGroupId)
                    .orElseThrow(() -> AppException.notFound("QuestionGroup", effectiveGroupId));
            if (req.getQuestionGroupId() != null) {
                Long effectiveSubjectId = req.getSubjectId() != null ? req.getSubjectId() : q.getSubjectId();
                if (!effectiveSubjectId.equals(resolvedGroup.getSubjectId())) {
                    throw AppException.badRequest("Question group subject must match question subject");
                }
            }
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
                    req.getOptions().stream().map(UpdateQuestionRequest.OptionInput::getIsCorrect).toList(),
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

        String resolvedAudioUrl = req.resolveAudioUrl();
        if (resolvedGroup != null) {
            if (req.getQuestionGroupId() != null) {
                q.setQuestionGroupId(req.getQuestionGroupId());
            }
            resolvedGroup = updateGroupAudioFields(resolvedGroup, resolvedAudioUrl,
                    req.resolveAudioDurationSec(), req.resolveMaxPlays());
        } else if (resolvedAudioUrl != null && !resolvedAudioUrl.isBlank()) {
            resolvedGroup = createAudioQuestionGroup(q.getSubjectId(), resolvedAudioUrl,
                    req.resolveAudioDurationSec(), req.resolveMaxPlays());
            if (resolvedGroup != null) {
                q.setQuestionGroupId(resolvedGroup.getId());
            }
        }

        q.setVersion(q.getVersion() + 1);
        Question saved = questionRepository.save(q);
        return toQuestionResponse(saved, resolvedGroup);
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
        Page<Question> page = status == null
                ? questionRepository.findAllByOrderByUpdatedAtDesc(pageable)
                : questionRepository.findByStatusOrderByUpdatedAtDesc(status, pageable);
        return page
                .map(this::toQuestionListItemResponse);
    }

    // ---------- PICKER ----------
    @Override
    public Page<QuestionPickerItemResponse> findForPicker(
            QuestionStatus status,
            Long subjectId,
            Long topicId,
            QuestionType questionType,
            String q,
            Pageable pageable) {
        QuestionStatus effectiveStatus = status != null ? status : QuestionStatus.APPROVED;
        Specification<Question> spec = Specification
                .where(QuestionSpecifications.hasStatus(effectiveStatus))
                .and(QuestionSpecifications.hasSubject(subjectId))
                .and(QuestionSpecifications.hasTopic(topicId))
                .and(QuestionSpecifications.hasQuestionType(questionType))
                .and(QuestionSpecifications.textContains(q));

        return questionRepository.findAll(spec, pageable)
                .map(this::toQuestionPickerItemResponse);
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
        if (type == null) {
            return;
        }
        long correctCount = isCorrectFlags == null ? 0 : isCorrectFlags.stream()
                .filter(Boolean.TRUE::equals)
                .count();

        switch (type) {
            case SINGLE_CHOICE -> {
                if (totalCount < 2) {
                    throw AppException.validationFailed(
                            "SINGLE_CHOICE requires at least 2 options, got " + totalCount);
                }
                if (correctCount != 1) {
                    throw AppException.validationFailed(
                            "SINGLE_CHOICE requires exactly 1 correct option, got " + correctCount);
                }
            }
            case MULTIPLE_CHOICE -> {
                if (totalCount < 2) {
                    throw AppException.validationFailed(
                            "MULTIPLE_CHOICE requires at least 2 options, got " + totalCount);
                }
                if (correctCount < 1) {
                    throw AppException.validationFailed(
                            "MULTIPLE_CHOICE requires at least 1 correct option");
                }
            }
            case TRUE_FALSE -> {
                if (totalCount < 2 || totalCount > 4) {
                    throw AppException.validationFailed(
                            "TRUE_FALSE requires between 2 and 4 options, got " + totalCount);
                }
            }
            case MATCHING -> {
                if (totalCount < 2 || totalCount > 6) {
                    throw AppException.validationFailed(
                            "MATCHING requires between 2 and 6 options, got " + totalCount);
                }
            }
            case SHORT_ANSWER -> {
                if (totalCount < 1 || totalCount > 10) {
                    throw AppException.validationFailed(
                            "SHORT_ANSWER requires between 1 and 10 options, got " + totalCount);
                }
                if (correctCount < 1) {
                    throw AppException.validationFailed(
                            "SHORT_ANSWER requires at least 1 correct option");
                }
            }
            case FILL_IN_BLANK -> {
                // No specific options constraint beyond request size
            }
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

    private String generateQuestionGroupCode() {
        return "QG-AUDIO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // ---------- MAPPERS ----------
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

    private QuestionResponse toQuestionResponse(Question q) {
        return toQuestionResponse(q, null);
    }

    private QuestionResponse toQuestionResponse(Question q, QuestionGroup preloadedGroup) {
        List<QuestionOption> options =
                questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(q.getId());
        List<QuestionReview> reviews =
                questionReviewRepository.findByQuestionIdOrderByCreatedAtDesc(q.getId());
        QuestionGroupResponse group = null;
        if (preloadedGroup != null) {
            group = toQuestionGroupResponse(preloadedGroup);
        } else if (q.getQuestionGroupId() != null) {
            group = questionGroupRepository.findById(q.getQuestionGroupId())
                    .map(this::toQuestionGroupResponse)
                    .orElse(null);
        }
        return QuestionResponse.builder()
                .id(q.getId())
                .questionCode(q.getQuestionCode())
                .subjectId(q.getSubjectId())
                .topicId(q.getTopicId())
                .subtopicId(q.getSubtopicId())
                .questionGroupId(q.getQuestionGroupId())
                .questionGroup(group)
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
                .lockVersion(q.getLockVersion())
                .createdBy(q.getCreatedBy())
                .reviewedBy(q.getReviewedBy())
                .createdAt(q.getCreatedAt())
                .updatedAt(q.getUpdatedAt())
                .options(options.stream().map(this::toOptionResponse).toList())
                .reviewHistory(reviews.stream().map(this::toReviewResponse).toList())
                .build();
    }

    private QuestionListItemResponse toQuestionListItemResponse(Question q) {
        String snippet = q.getQuestionText();
        if (snippet != null && snippet.length() > 120) {
            snippet = snippet.substring(0, 117) + "...";
        }
        return QuestionListItemResponse.builder()
                .id(q.getId())
                .questionCode(q.getQuestionCode())
                .questionTextSnippet(snippet)
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

    private QuestionPickerItemResponse toQuestionPickerItemResponse(Question q) {
        return QuestionPickerItemResponse.builder()
                .id(q.getId())
                .questionCode(q.getQuestionCode())
                .questionTextSnippet(toQuestionTextSnippet(q.getQuestionText()))
                .subjectId(q.getSubjectId())
                .topicId(q.getTopicId())
                .subtopicId(q.getSubtopicId())
                .questionType(q.getQuestionType())
                .difficulty(q.getDifficulty())
                .status(q.getStatus())
                .version(q.getVersion())
                .updatedAt(q.getUpdatedAt())
                .imageUrl(q.getImageUrl())
                .build();
    }

    private String toQuestionTextSnippet(String questionText) {
        if (questionText == null || questionText.length() <= 200) {
            return questionText;
        }
        return questionText.substring(0, 200) + "…";
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

    private QuestionGroup createAudioQuestionGroup(Long subjectId, String audioUrl, Integer audioDurationSec, Integer maxPlays) {
        QuestionGroup group = QuestionGroup.builder()
                .code(generateQuestionGroupCode())
                .subjectId(subjectId)
                .audioUrl(audioUrl)
                .audioDurationSec(audioDurationSec)
                .maxPlays(maxPlays != null ? maxPlays : 2)
                .build();
        QuestionGroup saved = questionGroupRepository.save(group);
        return saved != null ? saved : group;
    }

    private QuestionGroup updateGroupAudioFields(QuestionGroup group, String audioUrl, Integer audioDurationSec, Integer maxPlays) {
        boolean updated = false;
        if (audioUrl != null) {
            String normalizedAudioUrl = audioUrl.isBlank() ? null : audioUrl;
            if (!Objects.equals(group.getAudioUrl(), normalizedAudioUrl)) {
                group.setAudioUrl(normalizedAudioUrl);
                updated = true;
            }
        }
        if (audioDurationSec != null && !Objects.equals(group.getAudioDurationSec(), audioDurationSec)) {
            group.setAudioDurationSec(audioDurationSec);
            updated = true;
        }
        if (maxPlays != null) {
            if (!Objects.equals(group.getMaxPlays(), maxPlays)) {
                group.setMaxPlays(maxPlays);
                updated = true;
            }
        } else if (group.getMaxPlays() == null && audioUrl != null && !audioUrl.isBlank()) {
            group.setMaxPlays(2);
            updated = true;
        }
        if (updated) {
            QuestionGroup saved = questionGroupRepository.save(group);
            return saved != null ? saved : group;
        }
        return group;
    }
}
