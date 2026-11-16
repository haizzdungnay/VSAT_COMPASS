package com.vsatcompass.api.util;

import com.vsatcompass.api.entity.Exam;
import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.QuestionOption;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.entity.enums.QuestionType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Standard V-SAT 2026 exam format rules and structure validator.
 * Mirrors acceptance-harness/lib/vsat-format.mjs oracle.
 */
public final class VsatFormatRules {

    private VsatFormatRules() {}

    public static final List<QuestionType> PARTS = List.of(
            QuestionType.TRUE_FALSE,
            QuestionType.SINGLE_CHOICE,
            QuestionType.MATCHING,
            QuestionType.SHORT_ANSWER
    );

    public static final Map<QuestionType, Integer> PART_COUNTS = Map.of(
            QuestionType.TRUE_FALSE, 9,
            QuestionType.SINGLE_CHOICE, 6,
            QuestionType.MATCHING, 5,
            QuestionType.SHORT_ANSWER, 5
    );

    public static final int TOTAL_QUESTIONS = 25;
    public static final BigDecimal MAX_RAW = new BigDecimal("150.00");
    public static final BigDecimal DEFAULT_QUESTION_SCORE = new BigDecimal("6.00");
    public static final Set<String> NINETY_MIN_SUBJECTS = Set.of("MATH", "LIT");
    public static final String DEFAULT_SCORING_PROFILE_CODE = "VSAT_2026_DRAFT_SPEC_150";
    public static final Set<QuestionStatus> COMPOSABLE_STATUSES = Set.of(
            QuestionStatus.APPROVED,
            QuestionStatus.PUBLISHED
    );

    private static final List<String> TF_LABELS = List.of("a", "b", "c", "d");

    public record OptionView(Long id, String optionLabel, String optionText, Boolean isCorrect) {
        public OptionView(String optionLabel, String optionText, Boolean isCorrect) {
            this(null, optionLabel, optionText, isCorrect);
        }

        public static OptionView from(QuestionOption o) {
            if (o == null) return null;
            return new OptionView(o.getId(), o.getOptionLabel(), o.getOptionText(), o.getIsCorrect());
        }
    }

    public record ExamQuestionView(
            Long questionId,
            Long subjectId,
            QuestionType questionType,
            QuestionStatus status,
            BigDecimal maxRawScore,
            List<OptionView> options
    ) {
        public static ExamQuestionView from(Question q, BigDecimal maxRawScore, List<QuestionOption> options) {
            List<OptionView> opts = options == null ? List.of() : options.stream()
                    .filter(Objects::nonNull)
                    .map(OptionView::from)
                    .toList();
            return new ExamQuestionView(
                    q.getId(),
                    q.getSubjectId(),
                    q.getQuestionType(),
                    q.getStatus(),
                    maxRawScore != null ? maxRawScore : pointsFor(q.getQuestionType()),
                    opts
            );
        }
    }

    public static BigDecimal pointsFor(QuestionType type) {
        return DEFAULT_QUESTION_SCORE;
    }

    public static int partIndex(QuestionType type) {
        if (type == null) return -1;
        return PARTS.indexOf(type);
    }

    public static int expectedDuration(String subjectCode) {
        if (subjectCode != null && NINETY_MIN_SUBJECTS.contains(subjectCode.trim().toUpperCase())) {
            return 90;
        }
        return 60;
    }

    public static String stripMatchingTarget(String text) {
        if (text == null) return null;
        int arrowIdx = text.indexOf("⇄");
        if (arrowIdx >= 0) {
            return text.substring(0, arrowIdx).trim();
        }
        int asciiArrowIdx = text.indexOf("->");
        if (asciiArrowIdx >= 0) {
            return text.substring(0, asciiArrowIdx).trim();
        }
        return text;
    }

    public static String sanitizeOptionTextForStudent(String text, QuestionType questionType) {
        if (questionType == QuestionType.MATCHING) {
            return stripMatchingTarget(text);
        }
        if (questionType == QuestionType.SHORT_ANSWER) {
            return null;
        }
        return text;
    }

    public static Optional<QuestionType> findMismatchedPart(Map<QuestionType, Integer> counts) {
        if (counts == null) {
            return Optional.of(PARTS.get(0));
        }
        for (QuestionType part : PARTS) {
            if (!Objects.equals(counts.getOrDefault(part, 0), PART_COUNTS.get(part))) {
                return Optional.of(part);
            }
        }
        return Optional.empty();
    }

    public static boolean matchesVsatCounts(Integer totalQuestions, Map<QuestionType, Integer> counts) {
        return totalQuestions != null
                && totalQuestions == TOTAL_QUESTIONS
                && findMismatchedPart(counts).isEmpty();
    }

    public static String matchTarget(String text) {
        if (text == null) return "";
        String s = text.trim();
        if (s.contains("⇄")) {
            s = s.substring(s.indexOf("⇄") + 1).trim();
        } else if (s.contains("->")) {
            s = s.substring(s.indexOf("->") + 2).trim();
        }
        if (s.contains(":")) {
            s = s.substring(s.lastIndexOf(":") + 1).trim();
        }
        return s;
    }

    public static List<String> validateQuestionStructure(QuestionType type, List<OptionView> options) {
        List<OptionView> opts = options != null ? options : Collections.emptyList();
        if (type == null) {
            return Collections.emptyList();
        }
        switch (type) {
            case TRUE_FALSE -> {
                if (opts.size() != 4) {
                    return List.of("VF_TF4");
                }
                List<String> labels = opts.stream()
                        .map(o -> o.optionLabel() == null ? "" : o.optionLabel().trim().toLowerCase())
                        .sorted()
                        .toList();
                boolean labelsOk = TF_LABELS.equals(labels);
                boolean keysOk = opts.stream().allMatch(o -> o.isCorrect() != null);
                return (labelsOk && keysOk) ? Collections.emptyList() : List.of("VF_TF4");
            }
            case SINGLE_CHOICE -> {
                boolean countOk = opts.size() == 4;
                long correctCount = opts.stream().filter(o -> Boolean.TRUE.equals(o.isCorrect())).count();
                return (countOk && correctCount == 1) ? Collections.emptyList() : List.of("VF_MCQ4");
            }
            case MATCHING -> {
                boolean countOk = opts.size() == 4;
                long distinctLabels = opts.stream()
                        .map(o -> o.optionLabel() == null ? "" : o.optionLabel().trim())
                        .distinct()
                        .count();
                boolean keysOk = opts.stream().allMatch(o -> !matchTarget(o.optionText()).isEmpty());
                return (countOk && distinctLabels == 4 && keysOk) ? Collections.emptyList() : List.of("VF_MATCH");
            }
            case SHORT_ANSWER -> {
                Optional<OptionView> keyOpt = opts.stream()
                        .filter(o -> Boolean.TRUE.equals(o.isCorrect()) || opts.size() == 1)
                        .findFirst();
                boolean keyOk = keyOpt.isPresent()
                        && keyOpt.get().optionText() != null
                        && !keyOpt.get().optionText().trim().isEmpty();
                return keyOk ? Collections.emptyList() : List.of("VF_SA_KEY");
            }
            default -> {
                return Collections.emptyList();
            }
        }
    }

    public static List<String> validateQuestionStructure(Question question, List<QuestionOption> options) {
        if (question == null) return Collections.emptyList();
        List<OptionView> optViews = options == null ? List.of() : options.stream()
                .filter(Objects::nonNull)
                .map(OptionView::from)
                .toList();
        return validateQuestionStructure(question.getQuestionType(), optViews);
    }

    public static List<String> validateQuestionStructure(ExamQuestionView question) {
        if (question == null) return Collections.emptyList();
        return validateQuestionStructure(question.questionType(), question.options());
    }

    public static List<String> validateExam(
            Long examSubjectId,
            String examSubjectCode,
            Integer durationMinutes,
            boolean scoringProfileValid,
            List<ExamQuestionView> questions
    ) {
        Set<String> errs = new TreeSet<>();
        List<ExamQuestionView> qs = questions != null ? questions : Collections.emptyList();
        int n = qs.size();

        if (n != TOTAL_QUESTIONS) {
            errs.add("VF_COUNT");
        } else {
            Map<QuestionType, Integer> counts = new EnumMap<>(QuestionType.class);
            for (QuestionType p : PARTS) counts.put(p, 0);
            for (ExamQuestionView q : qs) {
                if (q.questionType() != null && counts.containsKey(q.questionType())) {
                    counts.put(q.questionType(), counts.get(q.questionType()) + 1);
                }
            }
            if (findMismatchedPart(counts).isPresent()) {
                errs.add("VF_MIX");
            }
            boolean scoresNotNull = qs.stream().allMatch(q -> q.maxRawScore() != null);
            if (scoresNotNull) {
                BigDecimal total = qs.stream()
                        .map(ExamQuestionView::maxRawScore)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (total.compareTo(MAX_RAW) != 0) {
                    errs.add("VF_TOTAL");
                }
            } else {
                errs.add("VF_TOTAL");
            }
        }

        int lastPart = -1;
        for (ExamQuestionView q : qs) {
            int p = partIndex(q.questionType());
            if (p < 0 || (lastPart >= 0 && p < lastPart)) {
                errs.add("VF_ORDER");
                break;
            }
            lastPart = p;
        }

        for (ExamQuestionView q : qs) {
            errs.addAll(validateQuestionStructure(q));
        }

        if (examSubjectId != null && qs.stream().anyMatch(q -> q.subjectId() != null && !q.subjectId().equals(examSubjectId))) {
            errs.add("VF_SUBJECT");
        }

        if (qs.stream().anyMatch(q -> q.status() != null && !COMPOSABLE_STATUSES.contains(q.status()))) {
            errs.add("VF_STATUS");
        }

        Set<Long> uniqueIds = new HashSet<>();
        for (ExamQuestionView q : qs) {
            if (q.questionId() != null && !uniqueIds.add(q.questionId())) {
                errs.add("VF_DUP");
                break;
            }
        }

        if (examSubjectCode != null) {
            int expected = expectedDuration(examSubjectCode);
            if (durationMinutes == null || !durationMinutes.equals(expected)) {
                errs.add("VF_DURATION");
            }
        }

        if (!scoringProfileValid) {
            errs.add("VF_PROFILE");
        }

        return new ArrayList<>(errs);
    }

    public static List<String> validateExam(
            Exam exam,
            String subjectCode,
            boolean scoringProfileValid,
            List<ExamQuestionView> questions
    ) {
        return validateExam(
                exam != null ? exam.getSubjectId() : null,
                subjectCode,
                exam != null ? exam.getDurationMinutes() : null,
                scoringProfileValid,
                questions
        );
    }
}
