package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.CreateQuestionRequest;
import com.vsatcompass.api.dto.request.ReviewActionRequest;
import com.vsatcompass.api.dto.request.UpdateQuestionRequest;
import com.vsatcompass.api.dto.response.QuestionListItemResponse;
import com.vsatcompass.api.dto.response.QuestionResponse;
import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.QuestionOption;
import com.vsatcompass.api.entity.QuestionReview;
import com.vsatcompass.api.entity.Subtopic;
import com.vsatcompass.api.entity.Topic;
import com.vsatcompass.api.entity.enums.Difficulty;
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
import com.vsatcompass.api.service.impl.QuestionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionService — Phase C1.1b Question Bank CRUD + workflow")
class QuestionServiceTest {

    @Mock QuestionRepository questionRepository;
    @Mock QuestionOptionRepository questionOptionRepository;
    @Mock QuestionReviewRepository questionReviewRepository;
    @Mock SubjectRepository subjectRepository;
    @Mock TopicRepository topicRepository;
    @Mock SubtopicRepository subtopicRepository;

    @InjectMocks QuestionServiceImpl questionService;

    // Constants
    private static final Long SUBJECT_MATH = 1L;
    private static final Long SUBJECT_OTHER = 2L;
    private static final Long TOPIC_ALGEBRA = 10L;
    private static final Long TOPIC_OTHER = 11L;
    private static final Long SUBTOPIC_LINEAR = 100L;

    private static final Long OWNER_ID = 501L;
    private static final Long OTHER_COLLAB_ID = 502L;
    private static final Long ADMIN_ID = 901L;

    private static final String ROLE_COLLAB = UserRole.COLLABORATOR.name();
    private static final String ROLE_CONTENT_ADMIN = UserRole.CONTENT_ADMIN.name();
    private static final String ROLE_SUPER_ADMIN = UserRole.SUPER_ADMIN.name();

    private Topic algebraTopic;
    private Subtopic linearSubtopic;

    @BeforeEach
    void setUp() {
        OffsetDateTime now = OffsetDateTime.now();

        algebraTopic = Topic.builder()
                .id(TOPIC_ALGEBRA).subjectId(SUBJECT_MATH).code("MATH_ALGEBRA").name("Đại số")
                .displayOrder(1).isActive(true).createdAt(now).updatedAt(now).build();

        linearSubtopic = Subtopic.builder()
                .id(SUBTOPIC_LINEAR).topicId(TOPIC_ALGEBRA).code("MATH_ALGEBRA_LINEAR")
                .name("Phương trình bậc 1").displayOrder(1).isActive(true)
                .createdAt(now).updatedAt(now).build();
    }

    // ============================================================
    // Helpers
    // ============================================================

    private CreateQuestionRequest validCreateReq(QuestionType type, List<Boolean> correctFlags) {
        List<CreateQuestionRequest.OptionInput> opts = correctFlags.stream()
                .map(isCorrect -> {
                    int idx = correctFlags.indexOf(isCorrect);
                    return CreateQuestionRequest.OptionInput.builder()
                            .optionLabel(String.valueOf((char) ('A' + idx)))
                            .optionText("Option " + idx)
                            .isCorrect(isCorrect)
                            .displayOrder(idx + 1)
                            .build();
                })
                .toList();
        return CreateQuestionRequest.builder()
                .subjectId(SUBJECT_MATH)
                .topicId(TOPIC_ALGEBRA)
                .subtopicId(SUBTOPIC_LINEAR)
                .difficulty(Difficulty.EASY)
                .questionType(type)
                .questionText("2 + 2 = ?")
                .options(opts)
                .build();
    }

    private CreateQuestionRequest validSingleChoiceReq() {
        // [false, true, false, false] — exactly 1 correct
        return validCreateReq(QuestionType.SINGLE_CHOICE, List.of(false, true, false, false));
    }

    private Question existingQuestion(Long id, QuestionStatus status, Long createdBy) {
        return Question.builder()
                .id(id)
                .questionCode("Q-T10-ABCD1234")
                .subjectId(SUBJECT_MATH)
                .topicId(TOPIC_ALGEBRA)
                .subtopicId(SUBTOPIC_LINEAR)
                .difficulty(Difficulty.EASY)
                .questionType(QuestionType.SINGLE_CHOICE)
                .questionText("Existing question")
                .status(status)
                .version(1)
                .createdBy(createdBy)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private void mockSubjectTopicValid() {
        when(subjectRepository.existsById(SUBJECT_MATH)).thenReturn(true);
        when(topicRepository.findById(TOPIC_ALGEBRA)).thenReturn(Optional.of(algebraTopic));
        when(subtopicRepository.findById(SUBTOPIC_LINEAR)).thenReturn(Optional.of(linearSubtopic));
    }

    // ============================================================
    // GROUP 1 — CREATE (5 tests)
    // ============================================================

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("create: happy path saves DRAFT question with createdBy + 4 options")
        void create_happyPath_returnsDraftWithCreatedBy() {
            mockSubjectTopicValid();
            when(questionRepository.save(any(Question.class)))
                    .thenAnswer(inv -> {
                        Question q = inv.getArgument(0);
                        q.setId(7777L);
                        return q;
                    });
            when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(7777L))
                    .thenReturn(Collections.emptyList());
            when(questionReviewRepository.findByQuestionIdOrderByCreatedAtDesc(7777L))
                    .thenReturn(Collections.emptyList());

            CreateQuestionRequest req = validSingleChoiceReq();
            QuestionResponse result = questionService.create(OWNER_ID, req);

            ArgumentCaptor<Question> qCap = ArgumentCaptor.forClass(Question.class);
            verify(questionRepository).save(qCap.capture());
            Question savedQ = qCap.getValue();
            assertThat(savedQ.getStatus()).isEqualTo(QuestionStatus.DRAFT);
            assertThat(savedQ.getCreatedBy()).isEqualTo(OWNER_ID);
            assertThat(savedQ.getVersion()).isEqualTo(1);
            assertThat(savedQ.getQuestionCode()).startsWith("Q-T" + TOPIC_ALGEBRA + "-");
            assertThat(savedQ.getSubjectId()).isEqualTo(SUBJECT_MATH);

            verify(questionOptionRepository, times(4)).save(any(QuestionOption.class));

            assertThat(result.getStatus()).isEqualTo(QuestionStatus.DRAFT);
            assertThat(result.getCreatedBy()).isEqualTo(OWNER_ID);
            assertThat(result.getId()).isEqualTo(7777L);
        }

        @Test
        @DisplayName("create: subject not found throws 404, no question saved")
        void create_subjectNotFound_throws404() {
            when(subjectRepository.existsById(SUBJECT_MATH)).thenReturn(false);

            assertThatThrownBy(() -> questionService.create(OWNER_ID, validSingleChoiceReq()))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException ae = (AppException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(ae.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
                        assertThat(ae.getMessage()).contains("Subject");
                    });

            verify(questionRepository, never()).save(any());
            verify(questionOptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("create: topic does not belong to subject throws 400 VALIDATION_FAILED")
        void create_topicNotInSubject_throws400Validation() {
            // Topic.subjectId = MATH (1), but request asserts subjectId = OTHER (2)
            CreateQuestionRequest req = validSingleChoiceReq();
            req.setSubjectId(SUBJECT_OTHER);

            when(subjectRepository.existsById(SUBJECT_OTHER)).thenReturn(true);
            when(topicRepository.findById(TOPIC_ALGEBRA)).thenReturn(Optional.of(algebraTopic));

            assertThatThrownBy(() -> questionService.create(OWNER_ID, req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException ae = (AppException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(ae.getCode()).isEqualTo("VALIDATION_FAILED");
                        assertThat(ae.getMessage()).contains("Topic " + TOPIC_ALGEBRA)
                                                   .contains("subject " + SUBJECT_OTHER);
                    });

            verify(questionRepository, never()).save(any());
        }

        @Test
        @DisplayName("create: SINGLE_CHOICE with zero correct options throws 400 VALIDATION_FAILED")
        void create_singleChoiceWithZeroCorrect_throws400() {
            mockSubjectTopicValid();
            CreateQuestionRequest req = validCreateReq(QuestionType.SINGLE_CHOICE,
                    List.of(false, false, false, false));

            assertThatThrownBy(() -> questionService.create(OWNER_ID, req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException ae = (AppException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(ae.getCode()).isEqualTo("VALIDATION_FAILED");
                        assertThat(ae.getMessage()).contains("SINGLE_CHOICE")
                                                   .contains("exactly 1 correct option");
                    });

            verify(questionRepository, never()).save(any());
        }

        @Test
        @DisplayName("create: MULTIPLE_CHOICE with all false throws 400 VALIDATION_FAILED")
        void create_multipleChoiceWithAllIncorrect_throws400() {
            mockSubjectTopicValid();
            CreateQuestionRequest req = validCreateReq(QuestionType.MULTIPLE_CHOICE,
                    List.of(false, false, false, false));

            assertThatThrownBy(() -> questionService.create(OWNER_ID, req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException ae = (AppException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(ae.getCode()).isEqualTo("VALIDATION_FAILED");
                        assertThat(ae.getMessage()).contains("MULTIPLE_CHOICE")
                                                   .contains("at least 1 correct option");
                    });

            verify(questionRepository, never()).save(any());
        }
    }

    // ============================================================
    // GROUP 2 — UPDATE (8 tests)
    // ============================================================

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("update: happy path by owner increments version, saves entity")
        void update_happyPath_byOwner_incrementsVersion() {
            Question existing = existingQuestion(50L, QuestionStatus.DRAFT, OWNER_ID);
            when(questionRepository.findById(50L)).thenReturn(Optional.of(existing));
            when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
            when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(50L))
                    .thenReturn(Collections.emptyList());
            when(questionReviewRepository.findByQuestionIdOrderByCreatedAtDesc(50L))
                    .thenReturn(Collections.emptyList());

            UpdateQuestionRequest req = UpdateQuestionRequest.builder()
                    .questionText("Updated text")
                    .build();

            QuestionResponse result = questionService.update(OWNER_ID, ROLE_COLLAB, 50L, req);

            assertThat(result.getVersion()).isEqualTo(2);
            assertThat(result.getQuestionText()).isEqualTo("Updated text");
            verify(questionOptionRepository, never()).deleteByQuestionId(any());
        }

        @Test
        @DisplayName("update: COLLABORATOR cannot edit another collaborator's draft, throws 403")
        void update_otherCollaborator_throws403_NoMutation() {
            Question existing = existingQuestion(50L, QuestionStatus.DRAFT, OWNER_ID);
            when(questionRepository.findById(50L)).thenReturn(Optional.of(existing));

            UpdateQuestionRequest req = UpdateQuestionRequest.builder()
                    .questionText("Hijack").build();

            assertThatThrownBy(() ->
                    questionService.update(OTHER_COLLAB_ID, ROLE_COLLAB, 50L, req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException ae = (AppException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                        assertThat(ae.getCode()).isEqualTo("FORBIDDEN");
                        assertThat(ae.getMessage()).contains("not the owner");
                    });

            verify(questionRepository, never()).save(any());
            verify(questionOptionRepository, never()).deleteByQuestionId(any());
        }

        @Test
        @DisplayName("update: CONTENT_ADMIN can edit any owner's question (admin override)")
        void update_byContentAdmin_succeedsForAnyOwner() {
            Question existing = existingQuestion(50L, QuestionStatus.DRAFT, OWNER_ID);
            when(questionRepository.findById(50L)).thenReturn(Optional.of(existing));
            when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
            when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(50L))
                    .thenReturn(Collections.emptyList());
            when(questionReviewRepository.findByQuestionIdOrderByCreatedAtDesc(50L))
                    .thenReturn(Collections.emptyList());

            UpdateQuestionRequest req = UpdateQuestionRequest.builder()
                    .questionText("Admin tweak").build();

            QuestionResponse result = questionService.update(ADMIN_ID, ROLE_CONTENT_ADMIN, 50L, req);

            assertThat(result.getQuestionText()).isEqualTo("Admin tweak");
            assertThat(result.getVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("update: PENDING_REVIEW status throws 409 INVALID_STATE")
        void update_onPendingReview_throws409_INVALID_STATE() {
            Question existing = existingQuestion(50L, QuestionStatus.PENDING_REVIEW, OWNER_ID);
            when(questionRepository.findById(50L)).thenReturn(Optional.of(existing));

            UpdateQuestionRequest req = UpdateQuestionRequest.builder()
                    .questionText("Try edit").build();

            assertThatThrownBy(() ->
                    questionService.update(OWNER_ID, ROLE_COLLAB, 50L, req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException ae = (AppException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(ae.getCode()).isEqualTo("INVALID_STATE");
                        assertThat(ae.getMessage()).contains("PENDING_REVIEW")
                                                   .contains("DRAFT or NEEDS_REVISION");
                    });

            verify(questionRepository, never()).save(any());
        }

        @Test
        @DisplayName("update: APPROVED status throws 409 INVALID_STATE")
        void update_onApproved_throws409_INVALID_STATE() {
            Question existing = existingQuestion(50L, QuestionStatus.APPROVED, OWNER_ID);
            when(questionRepository.findById(50L)).thenReturn(Optional.of(existing));

            UpdateQuestionRequest req = UpdateQuestionRequest.builder()
                    .questionText("Try edit approved").build();

            assertThatThrownBy(() ->
                    questionService.update(OWNER_ID, ROLE_COLLAB, 50L, req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException ae = (AppException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(ae.getCode()).isEqualTo("INVALID_STATE");
                    });

            verify(questionRepository, never()).save(any());
        }

        @Test
        @DisplayName("update: ARCHIVED status throws 409 INVALID_STATE")
        void update_onArchived_throws409_INVALID_STATE() {
            Question existing = existingQuestion(50L, QuestionStatus.ARCHIVED, OWNER_ID);
            when(questionRepository.findById(50L)).thenReturn(Optional.of(existing));

            UpdateQuestionRequest req = UpdateQuestionRequest.builder().questionText("X").build();

            assertThatThrownBy(() ->
                    questionService.update(OWNER_ID, ROLE_COLLAB, 50L, req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException ae = (AppException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(ae.getCode()).isEqualTo("INVALID_STATE");
                    });

            verify(questionRepository, never()).save(any());
        }

        @Test
        @DisplayName("update: when options non-null, deletes existing options and re-inserts atomically")
        void update_options_replacesAtomically() {
            Question existing = existingQuestion(50L, QuestionStatus.DRAFT, OWNER_ID);
            when(questionRepository.findById(50L)).thenReturn(Optional.of(existing));
            when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
            when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(50L))
                    .thenReturn(Collections.emptyList());
            when(questionReviewRepository.findByQuestionIdOrderByCreatedAtDesc(50L))
                    .thenReturn(Collections.emptyList());

            List<UpdateQuestionRequest.OptionInput> newOpts = List.of(
                    UpdateQuestionRequest.OptionInput.builder()
                            .optionLabel("A").optionText("New A").isCorrect(true).displayOrder(1).build(),
                    UpdateQuestionRequest.OptionInput.builder()
                            .optionLabel("B").optionText("New B").isCorrect(false).displayOrder(2).build()
            );
            UpdateQuestionRequest req = UpdateQuestionRequest.builder().options(newOpts).build();

            questionService.update(OWNER_ID, ROLE_COLLAB, 50L, req);

            verify(questionOptionRepository).deleteByQuestionId(50L);
            verify(questionOptionRepository, times(2)).save(any(QuestionOption.class));
        }

        @Test
        @DisplayName("update: changing type without replacement options validates existing options")
        void update_questionTypeChanged_withoutOptions_validatesExistingOptions() {
            Question existing = existingQuestion(50L, QuestionStatus.DRAFT, OWNER_ID);
            List<QuestionOption> existingOptions = List.of(
                    QuestionOption.builder()
                            .id(1L).questionId(50L).optionLabel("A").optionText("A")
                            .isCorrect(true).displayOrder(1).build(),
                    QuestionOption.builder()
                            .id(2L).questionId(50L).optionLabel("B").optionText("B")
                            .isCorrect(false).displayOrder(2).build(),
                    QuestionOption.builder()
                            .id(3L).questionId(50L).optionLabel("C").optionText("C")
                            .isCorrect(false).displayOrder(3).build()
            );
            when(questionRepository.findById(50L)).thenReturn(Optional.of(existing));
            when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(50L))
                    .thenReturn(existingOptions);

            UpdateQuestionRequest req = UpdateQuestionRequest.builder()
                    .questionType(QuestionType.TRUE_FALSE)
                    .build();

            assertThatThrownBy(() -> questionService.update(OWNER_ID, ROLE_COLLAB, 50L, req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException ae = (AppException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(ae.getCode()).isEqualTo("VALIDATION_FAILED");
                        assertThat(ae.getMessage()).contains("TRUE_FALSE")
                                                   .contains("exactly 2 options");
                    });

            verify(questionRepository, never()).save(any());
            verify(questionOptionRepository, never()).deleteByQuestionId(any());
        }
    }

    // ============================================================
    // GROUP 3 — SUBMIT FOR REVIEW (4 tests)
    // ============================================================

    @Nested
    @DisplayName("submitForReview()")
    class Submit {

        @Test
        @DisplayName("submit: from DRAFT by owner moves to PENDING_REVIEW")
        void submit_fromDraft_byOwner_movesToPendingReview() {
            Question existing = existingQuestion(60L, QuestionStatus.DRAFT, OWNER_ID);
            when(questionRepository.findById(60L)).thenReturn(Optional.of(existing));
            when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
            when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(60L))
                    .thenReturn(Collections.emptyList());
            when(questionReviewRepository.findByQuestionIdOrderByCreatedAtDesc(60L))
                    .thenReturn(Collections.emptyList());

            QuestionResponse result = questionService.submitForReview(OWNER_ID, 60L);

            assertThat(result.getStatus()).isEqualTo(QuestionStatus.PENDING_REVIEW);
            ArgumentCaptor<Question> cap = ArgumentCaptor.forClass(Question.class);
            verify(questionRepository).save(cap.capture());
            assertThat(cap.getValue().getStatus()).isEqualTo(QuestionStatus.PENDING_REVIEW);
        }

        @Test
        @DisplayName("submit: from NEEDS_REVISION by owner moves to PENDING_REVIEW")
        void submit_fromNeedsRevision_byOwner_movesToPendingReview() {
            Question existing = existingQuestion(60L, QuestionStatus.NEEDS_REVISION, OWNER_ID);
            when(questionRepository.findById(60L)).thenReturn(Optional.of(existing));
            when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
            when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(60L))
                    .thenReturn(Collections.emptyList());
            when(questionReviewRepository.findByQuestionIdOrderByCreatedAtDesc(60L))
                    .thenReturn(Collections.emptyList());

            QuestionResponse result = questionService.submitForReview(OWNER_ID, 60L);

            assertThat(result.getStatus()).isEqualTo(QuestionStatus.PENDING_REVIEW);
        }

        @Test
        @DisplayName("submit: by non-owner (even another COLLABORATOR) throws 403 FORBIDDEN")
        void submit_byNonOwner_throws403_evenForCollabRole() {
            Question existing = existingQuestion(60L, QuestionStatus.DRAFT, OWNER_ID);
            when(questionRepository.findById(60L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> questionService.submitForReview(OTHER_COLLAB_ID, 60L))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException ae = (AppException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                        assertThat(ae.getCode()).isEqualTo("FORBIDDEN");
                        assertThat(ae.getMessage()).contains("owner");
                    });

            verify(questionRepository, never()).save(any());
        }

        @Test
        @DisplayName("submit: from PENDING_REVIEW throws 409 INVALID_STATE")
        void submit_fromPendingReview_throws409_INVALID_STATE() {
            Question existing = existingQuestion(60L, QuestionStatus.PENDING_REVIEW, OWNER_ID);
            when(questionRepository.findById(60L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> questionService.submitForReview(OWNER_ID, 60L))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException ae = (AppException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(ae.getCode()).isEqualTo("INVALID_STATE");
                        assertThat(ae.getMessage()).contains("PENDING_REVIEW");
                    });

            verify(questionRepository, never()).save(any());
        }
    }

    // ============================================================
    // GROUP 4 — GET / READ ACCESS (3 tests)
    // ============================================================

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("getById: owner can read own question")
        void getById_byOwner_returnsResponse() {
            Question q = existingQuestion(70L, QuestionStatus.DRAFT, OWNER_ID);
            when(questionRepository.findById(70L)).thenReturn(Optional.of(q));
            when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(70L))
                    .thenReturn(Collections.emptyList());
            when(questionReviewRepository.findByQuestionIdOrderByCreatedAtDesc(70L))
                    .thenReturn(Collections.emptyList());

            QuestionResponse result = questionService.getById(OWNER_ID, ROLE_COLLAB, 70L);

            assertThat(result.getId()).isEqualTo(70L);
            assertThat(result.getCreatedBy()).isEqualTo(OWNER_ID);
        }

        @Test
        @DisplayName("getById: another COLLABORATOR cannot read someone else's question, throws 403")
        void getById_byOtherCollab_throws403() {
            Question q = existingQuestion(70L, QuestionStatus.DRAFT, OWNER_ID);
            when(questionRepository.findById(70L)).thenReturn(Optional.of(q));

            assertThatThrownBy(() -> questionService.getById(OTHER_COLLAB_ID, ROLE_COLLAB, 70L))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException ae = (AppException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                        assertThat(ae.getCode()).isEqualTo("FORBIDDEN");
                    });

            verifyNoInteractions(questionOptionRepository);
            verifyNoInteractions(questionReviewRepository);
        }

        @Test
        @DisplayName("getById: CONTENT_ADMIN can read any owner's question")
        void getById_byContentAdmin_anyOwnerSucceeds() {
            Question q = existingQuestion(70L, QuestionStatus.DRAFT, OWNER_ID);
            when(questionRepository.findById(70L)).thenReturn(Optional.of(q));
            when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(70L))
                    .thenReturn(Collections.emptyList());
            when(questionReviewRepository.findByQuestionIdOrderByCreatedAtDesc(70L))
                    .thenReturn(Collections.emptyList());

            QuestionResponse result = questionService.getById(ADMIN_ID, ROLE_CONTENT_ADMIN, 70L);

            assertThat(result.getId()).isEqualTo(70L);
            assertThat(result.getCreatedBy()).isEqualTo(OWNER_ID);
        }
    }

    // ============================================================
    // GROUP 5 — ADMIN ACTIONS (8 tests)
    // ============================================================

    @Nested
    @DisplayName("admin review actions")
    class AdminActions {

        @Test
        @DisplayName("approve: from PENDING_REVIEW sets APPROVED, records review row, sets reviewedBy")
        void approve_fromPendingReview_setsApproved_andRecordsReview() {
            Question q = existingQuestion(80L, QuestionStatus.PENDING_REVIEW, OWNER_ID);
            when(questionRepository.findById(80L)).thenReturn(Optional.of(q));
            when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
            when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(80L))
                    .thenReturn(Collections.emptyList());
            when(questionReviewRepository.findByQuestionIdOrderByCreatedAtDesc(80L))
                    .thenReturn(Collections.emptyList());

            QuestionResponse result = questionService.approve(ADMIN_ID, 80L,
                    ReviewActionRequest.builder().comment("Looks good").build());

            assertThat(result.getStatus()).isEqualTo(QuestionStatus.APPROVED);
            assertThat(result.getReviewedBy()).isEqualTo(ADMIN_ID);

            ArgumentCaptor<QuestionReview> revCap = ArgumentCaptor.forClass(QuestionReview.class);
            verify(questionReviewRepository).save(revCap.capture());
            QuestionReview rev = revCap.getValue();
            assertThat(rev.getAction()).isEqualTo(ReviewAction.APPROVE);
            assertThat(rev.getReviewerId()).isEqualTo(ADMIN_ID);
            assertThat(rev.getQuestionId()).isEqualTo(80L);
            assertThat(rev.getVersionReviewed()).isEqualTo(1);
            assertThat(rev.getComment()).isEqualTo("Looks good");
        }

        @Test
        @DisplayName("approve: from DRAFT throws 409 INVALID_STATE, no review row recorded")
        void approve_fromDraft_throws409() {
            Question q = existingQuestion(80L, QuestionStatus.DRAFT, OWNER_ID);
            when(questionRepository.findById(80L)).thenReturn(Optional.of(q));

            assertThatThrownBy(() -> questionService.approve(ADMIN_ID, 80L,
                    ReviewActionRequest.builder().build()))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException ae = (AppException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(ae.getCode()).isEqualTo("INVALID_STATE");
                        assertThat(ae.getMessage()).contains("APPROVE")
                                                   .contains("PENDING_REVIEW");
                    });

            verify(questionReviewRepository, never()).save(any());
            verify(questionRepository, never()).save(any());
        }

        @Test
        @DisplayName("requestRevision: with comment moves to NEEDS_REVISION + records review row")
        void requestRevision_withComment_setsNeedsRevision_andRecordsReview() {
            Question q = existingQuestion(80L, QuestionStatus.PENDING_REVIEW, OWNER_ID);
            when(questionRepository.findById(80L)).thenReturn(Optional.of(q));
            when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
            when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(80L))
                    .thenReturn(Collections.emptyList());
            when(questionReviewRepository.findByQuestionIdOrderByCreatedAtDesc(80L))
                    .thenReturn(Collections.emptyList());

            QuestionResponse result = questionService.requestRevision(ADMIN_ID, 80L,
                    ReviewActionRequest.builder().comment("Please clarify option B").build());

            assertThat(result.getStatus()).isEqualTo(QuestionStatus.NEEDS_REVISION);

            ArgumentCaptor<QuestionReview> revCap = ArgumentCaptor.forClass(QuestionReview.class);
            verify(questionReviewRepository).save(revCap.capture());
            assertThat(revCap.getValue().getAction()).isEqualTo(ReviewAction.REQUEST_REVISION);
            assertThat(revCap.getValue().getComment()).isEqualTo("Please clarify option B");
        }

        @Test
        @DisplayName("requestRevision: missing comment throws 400 VALIDATION_FAILED, no review recorded")
        void requestRevision_withoutComment_throws400_VALIDATION_FAILED() {
            Question q = existingQuestion(80L, QuestionStatus.PENDING_REVIEW, OWNER_ID);
            when(questionRepository.findById(80L)).thenReturn(Optional.of(q));

            assertThatThrownBy(() -> questionService.requestRevision(ADMIN_ID, 80L,
                    ReviewActionRequest.builder().comment("   ").build()))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException ae = (AppException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(ae.getCode()).isEqualTo("VALIDATION_FAILED");
                        assertThat(ae.getMessage()).contains("REQUEST_REVISION")
                                                   .contains("required");
                    });

            verify(questionReviewRepository, never()).save(any());
            verify(questionRepository, never()).save(any());
        }

        @Test
        @DisplayName("requestRevision: from APPROVED throws 409 INVALID_STATE")
        void requestRevision_fromApproved_throws409() {
            Question q = existingQuestion(80L, QuestionStatus.APPROVED, OWNER_ID);
            when(questionRepository.findById(80L)).thenReturn(Optional.of(q));

            assertThatThrownBy(() -> questionService.requestRevision(ADMIN_ID, 80L,
                    ReviewActionRequest.builder().comment("comment").build()))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException ae = (AppException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(ae.getCode()).isEqualTo("INVALID_STATE");
                    });

            verify(questionReviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("reject: with comment moves to ARCHIVED + records review row")
        void reject_withComment_setsArchived_andRecordsReview() {
            Question q = existingQuestion(80L, QuestionStatus.PENDING_REVIEW, OWNER_ID);
            when(questionRepository.findById(80L)).thenReturn(Optional.of(q));
            when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
            when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(80L))
                    .thenReturn(Collections.emptyList());
            when(questionReviewRepository.findByQuestionIdOrderByCreatedAtDesc(80L))
                    .thenReturn(Collections.emptyList());

            QuestionResponse result = questionService.reject(ADMIN_ID, 80L,
                    ReviewActionRequest.builder().comment("Out of scope").build());

            assertThat(result.getStatus()).isEqualTo(QuestionStatus.ARCHIVED);

            ArgumentCaptor<QuestionReview> revCap = ArgumentCaptor.forClass(QuestionReview.class);
            verify(questionReviewRepository).save(revCap.capture());
            assertThat(revCap.getValue().getAction()).isEqualTo(ReviewAction.REJECT);
            assertThat(revCap.getValue().getComment()).isEqualTo("Out of scope");
        }

        @Test
        @DisplayName("reject: missing comment throws 400 VALIDATION_FAILED, no review recorded")
        void reject_withoutComment_throws400_VALIDATION_FAILED() {
            Question q = existingQuestion(80L, QuestionStatus.PENDING_REVIEW, OWNER_ID);
            when(questionRepository.findById(80L)).thenReturn(Optional.of(q));

            assertThatThrownBy(() -> questionService.reject(ADMIN_ID, 80L,
                    ReviewActionRequest.builder().build()))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException ae = (AppException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(ae.getCode()).isEqualTo("VALIDATION_FAILED");
                        assertThat(ae.getMessage()).contains("REJECT").contains("required");
                    });

            verify(questionReviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("reject: from PUBLISHED throws 409 INVALID_STATE")
        void reject_fromPublished_throws409() {
            Question q = existingQuestion(80L, QuestionStatus.PUBLISHED, OWNER_ID);
            when(questionRepository.findById(80L)).thenReturn(Optional.of(q));

            assertThatThrownBy(() -> questionService.reject(ADMIN_ID, 80L,
                    ReviewActionRequest.builder().comment("nope").build()))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException ae = (AppException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(ae.getCode()).isEqualTo("INVALID_STATE");
                    });

            verify(questionReviewRepository, never()).save(any());
        }
    }

    // ============================================================
    // GROUP 6 — LISTING (2 tests)
    // ============================================================

    @Nested
    @DisplayName("listing")
    class Listing {

        @Test
        @DisplayName("listByCreator: without status filter calls findByCreatedByOrderByUpdatedAtDesc")
        void listByCreator_withoutStatusFilter_callsBaseQuery() {
            Pageable pageable = PageRequest.of(0, 20);
            Question q = existingQuestion(90L, QuestionStatus.DRAFT, OWNER_ID);
            Page<Question> page = new PageImpl<>(List.of(q), pageable, 1);
            when(questionRepository.findByCreatedByOrderByUpdatedAtDesc(OWNER_ID, pageable))
                    .thenReturn(page);

            Page<QuestionListItemResponse> result = questionService.listByCreator(OWNER_ID, null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(90L);
            verify(questionRepository).findByCreatedByOrderByUpdatedAtDesc(OWNER_ID, pageable);
            verify(questionRepository, never())
                    .findByCreatedByAndStatusOrderByUpdatedAtDesc(any(), any(), any());
        }

        @Test
        @DisplayName("listByCreator: with status filter calls findByCreatedByAndStatusOrderByUpdatedAtDesc")
        void listByCreator_withStatusFilter_callsFilteredQuery() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Question> page = new PageImpl<>(Collections.emptyList(), pageable, 0);
            when(questionRepository.findByCreatedByAndStatusOrderByUpdatedAtDesc(
                    OWNER_ID, QuestionStatus.PENDING_REVIEW, pageable))
                    .thenReturn(page);

            Page<QuestionListItemResponse> result = questionService.listByCreator(
                    OWNER_ID, QuestionStatus.PENDING_REVIEW, pageable);

            assertThat(result.getTotalElements()).isZero();
            verify(questionRepository).findByCreatedByAndStatusOrderByUpdatedAtDesc(
                    OWNER_ID, QuestionStatus.PENDING_REVIEW, pageable);
            verify(questionRepository, never())
                    .findByCreatedByOrderByUpdatedAtDesc(any(), any());
        }
    }

    // ============================================================
    // GROUP 7 — Forbidden state-machine transitions (3 extra tests)
    // ============================================================

    @Nested
    @DisplayName("forbidden state-machine transitions")
    class ForbiddenTransitions {

        @Test
        @DisplayName("update: PUBLISHED status throws 409 INVALID_STATE")
        void update_onPublished_throws409_INVALID_STATE() {
            Question existing = existingQuestion(50L, QuestionStatus.PUBLISHED, OWNER_ID);
            when(questionRepository.findById(50L)).thenReturn(Optional.of(existing));

            UpdateQuestionRequest req = UpdateQuestionRequest.builder().questionText("x").build();

            assertThatThrownBy(() ->
                    questionService.update(OWNER_ID, ROLE_COLLAB, 50L, req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException ae = (AppException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(ae.getCode()).isEqualTo("INVALID_STATE");
                    });
        }

        @Test
        @DisplayName("update: HIDDEN status throws 409 INVALID_STATE")
        void update_onHidden_throws409_INVALID_STATE() {
            Question existing = existingQuestion(50L, QuestionStatus.HIDDEN, OWNER_ID);
            when(questionRepository.findById(50L)).thenReturn(Optional.of(existing));

            UpdateQuestionRequest req = UpdateQuestionRequest.builder().questionText("x").build();

            assertThatThrownBy(() ->
                    questionService.update(OWNER_ID, ROLE_SUPER_ADMIN, 50L, req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException ae = (AppException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(ae.getCode()).isEqualTo("INVALID_STATE");
                    });
        }

        @Test
        @DisplayName("submit: from APPROVED throws 409 INVALID_STATE")
        void submit_fromApproved_throws409_INVALID_STATE() {
            Question existing = existingQuestion(60L, QuestionStatus.APPROVED, OWNER_ID);
            when(questionRepository.findById(60L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> questionService.submitForReview(OWNER_ID, 60L))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException ae = (AppException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(ae.getCode()).isEqualTo("INVALID_STATE");
                    });

            verify(questionRepository, never()).save(any());
        }
    }
}
