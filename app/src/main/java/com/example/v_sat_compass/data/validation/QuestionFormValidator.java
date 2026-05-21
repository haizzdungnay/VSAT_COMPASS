package com.example.v_sat_compass.data.validation;

import com.example.v_sat_compass.data.model.enums.QuestionType;
import com.example.v_sat_compass.data.model.question.CreateQuestionRequest;
import com.example.v_sat_compass.data.model.question.QuestionOptionInput;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QuestionFormValidator {

    public static final String FIELD_SUBJECT_ID = "subjectId";
    public static final String FIELD_TOPIC_ID = "topicId";
    public static final String FIELD_DIFFICULTY = "difficulty";
    public static final String FIELD_QUESTION_TYPE = "questionType";
    public static final String FIELD_QUESTION_TEXT = "questionText";
    public static final String FIELD_EXPLANATION = "explanation";
    public static final String FIELD_SOURCE = "source";
    public static final String FIELD_TAGS = "tags";
    public static final String FIELD_IMAGE_URL = "imageUrl";
    public static final String FIELD_OPTIONS = "options";
    public static final String ERROR_REQUIRED = "required";
    public static final String ERROR_TOO_LONG = "too_long";
    public static final String ERROR_OPTIONS_MIN = "options_min";
    public static final String ERROR_OPTIONS_MAX = "options_max";
    public static final String ERROR_SINGLE_CHOICE_CORRECT = "single_choice_correct";
    public static final String ERROR_MULTI_CHOICE_CORRECT = "multi_choice_correct";
    public static final String ERROR_TRUE_FALSE_CORRECT = "true_false_correct";
    public static final String ERROR_TRUE_FALSE_SIZE = "true_false_size";

    private static final int QUESTION_TEXT_MAX = 5000;
    private static final int EXPLANATION_MAX = 5000;
    private static final int SOURCE_MAX = 200;
    private static final int TAGS_MAX = 500;
    private static final int IMAGE_URL_MAX = 500;
    private static final int OPTION_TEXT_MAX = 2000;
    private static final int OPTIONS_MIN = 2;
    private static final int OPTIONS_MAX = 10;

    public ValidationResult validateCreate(CreateQuestionRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        String formError = null;

        if (request == null) {
            errors.put(FIELD_QUESTION_TEXT, ERROR_REQUIRED);
            errors.put(FIELD_OPTIONS, ERROR_OPTIONS_MIN);
            return new ValidationResult(false, errors, null);
        }

        require(request.getSubjectId(), FIELD_SUBJECT_ID, errors);
        require(request.getTopicId(), FIELD_TOPIC_ID, errors);
        require(request.getDifficulty(), FIELD_DIFFICULTY, errors);
        require(request.getQuestionType(), FIELD_QUESTION_TYPE, errors);
        validateRequiredText(request.getQuestionText(), FIELD_QUESTION_TEXT,
                QUESTION_TEXT_MAX, errors);
        validateOptionalText(request.getExplanation(), FIELD_EXPLANATION,
                EXPLANATION_MAX, errors);
        validateOptionalText(request.getSource(), FIELD_SOURCE, SOURCE_MAX, errors);
        validateOptionalText(request.getTags(), FIELD_TAGS, TAGS_MAX, errors);
        validateOptionalText(request.getImageUrl(), FIELD_IMAGE_URL, IMAGE_URL_MAX, errors);

        List<QuestionOptionInput> options = request.getOptions();
        if (options == null || options.size() < OPTIONS_MIN) {
            errors.put(FIELD_OPTIONS, ERROR_OPTIONS_MIN);
        } else if (options.size() > OPTIONS_MAX) {
            errors.put(FIELD_OPTIONS, ERROR_OPTIONS_MAX);
        }

        int correctCount = 0;
        if (options != null) {
            for (int i = 0; i < options.size(); i++) {
                QuestionOptionInput option = options.get(i);
                if (option == null) {
                    errors.put(optionTextKey(i), ERROR_REQUIRED);
                    continue;
                }
                validateRequiredText(option.getOptionText(), optionTextKey(i),
                        OPTION_TEXT_MAX, errors);
                if (Boolean.TRUE.equals(option.getIsCorrect())) {
                    correctCount++;
                }
            }
        }

        QuestionType type = request.getQuestionType();
        if (type == QuestionType.SINGLE_CHOICE && correctCount != 1) {
            formError = ERROR_SINGLE_CHOICE_CORRECT;
        } else if (type == QuestionType.MULTIPLE_CHOICE && correctCount < 1) {
            formError = ERROR_MULTI_CHOICE_CORRECT;
        } else if (type == QuestionType.TRUE_FALSE) {
            if (options == null || options.size() != 2) {
                formError = ERROR_TRUE_FALSE_SIZE;
            } else if (correctCount != 1) {
                formError = ERROR_TRUE_FALSE_CORRECT;
            }
        }

        return new ValidationResult(errors.isEmpty() && formError == null, errors, formError);
    }

    public static String optionTextKey(int index) {
        return FIELD_OPTIONS + "[" + index + "].optionText";
    }

    private static void require(Object value, String field, Map<String, String> errors) {
        if (value == null) {
            errors.put(field, ERROR_REQUIRED);
        }
    }

    private static void validateRequiredText(
            String value,
            String field,
            int maxLength,
            Map<String, String> errors
    ) {
        if (value == null || value.trim().isEmpty()) {
            errors.put(field, ERROR_REQUIRED);
        } else if (value.length() > maxLength) {
            errors.put(field, ERROR_TOO_LONG);
        }
    }

    private static void validateOptionalText(
            String value,
            String field,
            int maxLength,
            Map<String, String> errors
    ) {
        if (value != null && value.length() > maxLength) {
            errors.put(field, ERROR_TOO_LONG);
        }
    }

    public static final class ValidationResult {
        private final boolean valid;
        private final Map<String, String> fieldErrors;
        private final String formError;

        ValidationResult(boolean valid, Map<String, String> fieldErrors, String formError) {
            this.valid = valid;
            this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
            this.formError = formError;
        }

        public boolean isValid() {
            return valid;
        }

        public Map<String, String> getFieldErrors() {
            return fieldErrors;
        }

        public String getFormError() {
            return formError;
        }
    }
}
