package com.vsatcompass.api.util;

import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.entity.enums.QuestionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VsatFormatRules — V-SAT Exam Format & Question Structure Rules")
class VsatFormatRulesTest {

    private static final Long SUBJECT_ID = 1L;
    private static final String SUBJECT_CODE = "MATH";

    private List<VsatFormatRules.OptionView> validTfOptions() {
        return List.of(
                new VsatFormatRules.OptionView("a", "Ý 1", true),
                new VsatFormatRules.OptionView("b", "Ý 2", false),
                new VsatFormatRules.OptionView("c", "Ý 3", true),
                new VsatFormatRules.OptionView("d", "Ý 4", false)
        );
    }

    private List<VsatFormatRules.OptionView> validMcqOptions() {
        return List.of(
                new VsatFormatRules.OptionView("A", "Đáp án A", true),
                new VsatFormatRules.OptionView("B", "Đáp án B", false),
                new VsatFormatRules.OptionView("C", "Đáp án C", false),
                new VsatFormatRules.OptionView("D", "Đáp án D", false)
        );
    }

    private List<VsatFormatRules.OptionView> validMatchingOptions() {
        return List.of(
                new VsatFormatRules.OptionView("1", "A ⇄ 1", true),
                new VsatFormatRules.OptionView("2", "B ⇄ 2", true),
                new VsatFormatRules.OptionView("3", "C ⇄ 3", true),
                new VsatFormatRules.OptionView("4", "D ⇄ 4", true)
        );
    }

    private List<VsatFormatRules.OptionView> validSaOptions() {
        return List.of(
                new VsatFormatRules.OptionView("1", "42", true)
        );
    }

    private List<VsatFormatRules.ExamQuestionView> buildStandard25Questions(Long subjectId) {
        List<VsatFormatRules.ExamQuestionView> list = new ArrayList<>();
        long qid = 1L;

        // 9 TF
        for (int i = 0; i < 9; i++) {
            list.add(new VsatFormatRules.ExamQuestionView(
                    qid++, subjectId, QuestionType.TRUE_FALSE, QuestionStatus.APPROVED,
                    new BigDecimal("6.00"), validTfOptions()));
        }
        // 6 MCQ
        for (int i = 0; i < 6; i++) {
            list.add(new VsatFormatRules.ExamQuestionView(
                    qid++, subjectId, QuestionType.SINGLE_CHOICE, QuestionStatus.APPROVED,
                    new BigDecimal("6.00"), validMcqOptions()));
        }
        // 5 MATCHING
        for (int i = 0; i < 5; i++) {
            list.add(new VsatFormatRules.ExamQuestionView(
                    qid++, subjectId, QuestionType.MATCHING, QuestionStatus.APPROVED,
                    new BigDecimal("6.00"), validMatchingOptions()));
        }
        // 5 SHORT_ANSWER
        for (int i = 0; i < 5; i++) {
            list.add(new VsatFormatRules.ExamQuestionView(
                    qid++, subjectId, QuestionType.SHORT_ANSWER, QuestionStatus.APPROVED,
                    new BigDecimal("6.00"), validSaOptions()));
        }
        return list;
    }

    @Test
    @DisplayName("pointsFor returns 6.00 for all question types")
    void pointsFor_allTypes_returnsSix() {
        for (QuestionType type : QuestionType.values()) {
            assertThat(VsatFormatRules.pointsFor(type)).isEqualByComparingTo("6.00");
        }
    }

    @Test
    @DisplayName("validateExam: standard 25-question exam produces NO errors")
    void validateExam_standardExam_noErrors() {
        List<VsatFormatRules.ExamQuestionView> questions = buildStandard25Questions(SUBJECT_ID);
        List<String> errors = VsatFormatRules.validateExam(
                SUBJECT_ID, SUBJECT_CODE, 90, true, questions
        );
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("validateExam: question with mismatched subjectId produces VF_SUBJECT")
    void validateExam_subjectMismatch_producesError() {
        List<VsatFormatRules.ExamQuestionView> questions = buildStandard25Questions(SUBJECT_ID);
        // Change subject of question 0
        VsatFormatRules.ExamQuestionView q0 = questions.get(0);
        questions.set(0, new VsatFormatRules.ExamQuestionView(
                q0.questionId(), 999L, q0.questionType(), q0.status(),
                q0.maxRawScore(), q0.options()));

        List<String> errors = VsatFormatRules.validateExam(
                SUBJECT_ID, SUBJECT_CODE, 90, true, questions
        );
        assertThat(errors).contains("VF_SUBJECT");
    }

    @Test
    @DisplayName("validateExam: incorrect duration produces VF_DURATION")
    void validateExam_invalidDuration_producesError() {
        List<VsatFormatRules.ExamQuestionView> questions = buildStandard25Questions(SUBJECT_ID);
        List<String> errors = VsatFormatRules.validateExam(
                SUBJECT_ID, SUBJECT_CODE, 60, true, questions
        );
        assertThat(errors).contains("VF_DURATION");
    }

    @Test
    @DisplayName("validateExam: missing scoring profile produces VF_PROFILE")
    void validateExam_noProfile_producesError() {
        List<VsatFormatRules.ExamQuestionView> questions = buildStandard25Questions(SUBJECT_ID);
        List<String> errors = VsatFormatRules.validateExam(
                SUBJECT_ID, SUBJECT_CODE, 90, false, questions
        );
        assertThat(errors).contains("VF_PROFILE");
    }

    @Test
    @DisplayName("validateExam: total questions != 25 produces VF_COUNT")
    void validateExam_wrongTotalCount_producesError() {
        List<VsatFormatRules.ExamQuestionView> questions = buildStandard25Questions(SUBJECT_ID);
        questions.remove(questions.size() - 1); // 24 questions

        List<String> errors = VsatFormatRules.validateExam(
                SUBJECT_ID, SUBJECT_CODE, 90, true, questions
        );
        assertThat(errors).contains("VF_COUNT");
    }

    @Test
    @DisplayName("validateExam: section sequence violation produces VF_ORDER")
    void validateExam_wrongOrder_producesError() {
        List<VsatFormatRules.ExamQuestionView> questions = buildStandard25Questions(SUBJECT_ID);
        // Swap first TF (index 0) with first MCQ (index 9)
        VsatFormatRules.ExamQuestionView temp = questions.get(0);
        questions.set(0, questions.get(9));
        questions.set(9, temp);

        List<String> errors = VsatFormatRules.validateExam(
                SUBJECT_ID, SUBJECT_CODE, 90, true, questions
        );
        assertThat(errors).contains("VF_ORDER");
    }

    @Test
    @DisplayName("validateExam: wrong part distribution produces VF_MIX")
    void validateExam_wrongPartCount_producesError() {
        List<VsatFormatRules.ExamQuestionView> questions = new ArrayList<>();
        // 25 MCQ questions
        for (int i = 0; i < 25; i++) {
            questions.add(new VsatFormatRules.ExamQuestionView(
                    (long) (i + 1), SUBJECT_ID, QuestionType.SINGLE_CHOICE, QuestionStatus.APPROVED,
                    new BigDecimal("6.00"), validMcqOptions()));
        }

        List<String> errors = VsatFormatRules.validateExam(
                SUBJECT_ID, SUBJECT_CODE, 90, true, questions
        );
        assertThat(errors).contains("VF_MIX");
    }

    @Test
    @DisplayName("validateQuestionStructure: TRUE_FALSE structure validations")
    void validateQuestionStructure_trueFalse() {
        // Valid
        assertThat(VsatFormatRules.validateQuestionStructure(QuestionType.TRUE_FALSE, validTfOptions()))
                .isEmpty();

        // 3 options -> VF_TF4
        assertThat(VsatFormatRules.validateQuestionStructure(QuestionType.TRUE_FALSE, validTfOptions().subList(0, 3)))
                .contains("VF_TF4");

        // Wrong labels -> VF_TF4
        List<VsatFormatRules.OptionView> badLabels = List.of(
                new VsatFormatRules.OptionView("1", "Ý 1", true),
                new VsatFormatRules.OptionView("2", "Ý 2", false),
                new VsatFormatRules.OptionView("3", "Ý 3", true),
                new VsatFormatRules.OptionView("4", "Ý 4", false)
        );
        assertThat(VsatFormatRules.validateQuestionStructure(QuestionType.TRUE_FALSE, badLabels))
                .contains("VF_TF4");
    }

    @Test
    @DisplayName("validateQuestionStructure: SINGLE_CHOICE structure validations")
    void validateQuestionStructure_singleChoice() {
        // Valid
        assertThat(VsatFormatRules.validateQuestionStructure(QuestionType.SINGLE_CHOICE, validMcqOptions()))
                .isEmpty();

        // 3 options -> VF_MCQ4
        assertThat(VsatFormatRules.validateQuestionStructure(QuestionType.SINGLE_CHOICE, validMcqOptions().subList(0, 3)))
                .contains("VF_MCQ4");

        // 0 correct -> VF_MCQ4
        List<VsatFormatRules.OptionView> noCorrect = List.of(
                new VsatFormatRules.OptionView("A", "A", false),
                new VsatFormatRules.OptionView("B", "B", false),
                new VsatFormatRules.OptionView("C", "C", false),
                new VsatFormatRules.OptionView("D", "D", false)
        );
        assertThat(VsatFormatRules.validateQuestionStructure(QuestionType.SINGLE_CHOICE, noCorrect))
                .contains("VF_MCQ4");
    }

    @Test
    @DisplayName("validateQuestionStructure: MATCHING structure validations")
    void validateQuestionStructure_matching() {
        // Valid
        assertThat(VsatFormatRules.validateQuestionStructure(QuestionType.MATCHING, validMatchingOptions()))
                .isEmpty();

        // Duplicate labels or empty match target -> VF_MATCH
        List<VsatFormatRules.OptionView> badMatch = List.of(
                new VsatFormatRules.OptionView("1", "A ⇄ 1", true),
                new VsatFormatRules.OptionView("1", "B ⇄ 2", true),
                new VsatFormatRules.OptionView("3", "C ⇄ 3", true),
                new VsatFormatRules.OptionView("4", "D ⇄ 4", true)
        );
        assertThat(VsatFormatRules.validateQuestionStructure(QuestionType.MATCHING, badMatch))
                .contains("VF_MATCH");

        List<VsatFormatRules.OptionView> emptyTargetMatch = List.of(
                new VsatFormatRules.OptionView("1", "A ⇄ ", true),
                new VsatFormatRules.OptionView("2", "B ⇄ 2", true),
                new VsatFormatRules.OptionView("3", "C ⇄ 3", true),
                new VsatFormatRules.OptionView("4", "D ⇄ 4", true)
        );
        assertThat(VsatFormatRules.validateQuestionStructure(QuestionType.MATCHING, emptyTargetMatch))
                .contains("VF_MATCH");
    }

    @Test
    @DisplayName("validateQuestionStructure: SHORT_ANSWER structure validations")
    void validateQuestionStructure_shortAnswer() {
        // Valid
        assertThat(VsatFormatRules.validateQuestionStructure(QuestionType.SHORT_ANSWER, validSaOptions()))
                .isEmpty();

        // No key / empty -> VF_SA_KEY
        assertThat(VsatFormatRules.validateQuestionStructure(QuestionType.SHORT_ANSWER, List.of()))
                .contains("VF_SA_KEY");

        List<VsatFormatRules.OptionView> emptyText = List.of(
                new VsatFormatRules.OptionView("1", "   ", true)
        );
        assertThat(VsatFormatRules.validateQuestionStructure(QuestionType.SHORT_ANSWER, emptyText))
                .contains("VF_SA_KEY");
    }

    @Test
    @DisplayName("sanitizeOptionTextForStudent and matchesVsatCounts handle edge cases")
    void sanitizeAndMatchesVsatCounts() {
        assertThat(VsatFormatRules.sanitizeOptionTextForStudent("Mục 1 ⇄ C", QuestionType.MATCHING))
                .isEqualTo("Mục 1");
        assertThat(VsatFormatRules.sanitizeOptionTextForStudent("Mục 2 -> B", QuestionType.MATCHING))
                .isEqualTo("Mục 2");
        assertThat(VsatFormatRules.sanitizeOptionTextForStudent("42.5", QuestionType.SHORT_ANSWER))
                .isNull();
        assertThat(VsatFormatRules.sanitizeOptionTextForStudent("Option A", QuestionType.SINGLE_CHOICE))
                .isEqualTo("Option A");

        assertThat(VsatFormatRules.matchesVsatCounts(25, VsatFormatRules.PART_COUNTS)).isTrue();
        assertThat(VsatFormatRules.matchesVsatCounts(24, VsatFormatRules.PART_COUNTS)).isFalse();
        assertThat(VsatFormatRules.findMismatchedPart(VsatFormatRules.PART_COUNTS)).isEmpty();
    }
}
