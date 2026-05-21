package com.example.v_sat_compass.data.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.v_sat_compass.data.model.enums.Difficulty;
import com.example.v_sat_compass.data.model.enums.QuestionType;
import com.example.v_sat_compass.data.model.question.CreateQuestionRequest;
import com.example.v_sat_compass.data.model.question.QuestionOptionInput;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuestionFormValidatorTest {

    private QuestionFormValidator validator;

    @Before
    public void setUp() {
        validator = new QuestionFormValidator();
    }

    @Test
    public void validSingleChoice_passes() {
        assertTrue(validator.validateCreate(validRequest(
                QuestionType.SINGLE_CHOICE,
                option("A", true),
                option("B", false)
        )).isValid());
    }

    @Test
    public void validMultipleChoice_passes() {
        assertTrue(validator.validateCreate(validRequest(
                QuestionType.MULTIPLE_CHOICE,
                option("A", true),
                option("B", true),
                option("C", false)
        )).isValid());
    }

    @Test
    public void validTrueFalse_passes() {
        assertTrue(validator.validateCreate(validRequest(
                QuestionType.TRUE_FALSE,
                option("Đ", true),
                option("S", false)
        )).isValid());
    }

    @Test
    public void missingSubjectId_fails() {
        CreateQuestionRequest request = validRequest();
        request.setSubjectId(null);

        assertFieldError(request, QuestionFormValidator.FIELD_SUBJECT_ID,
                QuestionFormValidator.ERROR_REQUIRED);
    }

    @Test
    public void missingTopicId_fails() {
        CreateQuestionRequest request = validRequest();
        request.setTopicId(null);

        assertFieldError(request, QuestionFormValidator.FIELD_TOPIC_ID,
                QuestionFormValidator.ERROR_REQUIRED);
    }

    @Test
    public void missingDifficulty_fails() {
        CreateQuestionRequest request = validRequest();
        request.setDifficulty(null);

        assertFieldError(request, QuestionFormValidator.FIELD_DIFFICULTY,
                QuestionFormValidator.ERROR_REQUIRED);
    }

    @Test
    public void missingQuestionType_fails() {
        CreateQuestionRequest request = validRequest();
        request.setQuestionType(null);

        assertFieldError(request, QuestionFormValidator.FIELD_QUESTION_TYPE,
                QuestionFormValidator.ERROR_REQUIRED);
    }

    @Test
    public void blankQuestionText_fails() {
        CreateQuestionRequest request = validRequest();
        request.setQuestionText("   ");

        assertFieldError(request, QuestionFormValidator.FIELD_QUESTION_TEXT,
                QuestionFormValidator.ERROR_REQUIRED);
    }

    @Test
    public void questionTextExactly5000_passes() {
        CreateQuestionRequest request = validRequest();
        request.setQuestionText(repeat('a', 5000));

        assertTrue(validator.validateCreate(request).isValid());
    }

    @Test
    public void questionText5001_fails() {
        CreateQuestionRequest request = validRequest();
        request.setQuestionText(repeat('a', 5001));

        assertFieldError(request, QuestionFormValidator.FIELD_QUESTION_TEXT,
                QuestionFormValidator.ERROR_TOO_LONG);
    }

    @Test
    public void explanation5001_fails() {
        CreateQuestionRequest request = validRequest();
        request.setExplanation(repeat('a', 5001));

        assertFieldError(request, QuestionFormValidator.FIELD_EXPLANATION,
                QuestionFormValidator.ERROR_TOO_LONG);
    }

    @Test
    public void source201_fails() {
        CreateQuestionRequest request = validRequest();
        request.setSource(repeat('a', 201));

        assertFieldError(request, QuestionFormValidator.FIELD_SOURCE,
                QuestionFormValidator.ERROR_TOO_LONG);
    }

    @Test
    public void tags501_fails() {
        CreateQuestionRequest request = validRequest();
        request.setTags(repeat('a', 501));

        assertFieldError(request, QuestionFormValidator.FIELD_TAGS,
                QuestionFormValidator.ERROR_TOO_LONG);
    }

    @Test
    public void imageUrl501_fails() {
        CreateQuestionRequest request = validRequest();
        request.setImageUrl(repeat('a', 501));

        assertFieldError(request, QuestionFormValidator.FIELD_IMAGE_URL,
                QuestionFormValidator.ERROR_TOO_LONG);
    }

    @Test
    public void optionsLessThanTwo_fails() {
        CreateQuestionRequest request = validRequest(
                QuestionType.SINGLE_CHOICE,
                option("A", true)
        );

        assertFieldError(request, QuestionFormValidator.FIELD_OPTIONS,
                QuestionFormValidator.ERROR_OPTIONS_MIN);
    }

    @Test
    public void optionsMoreThanTen_fails() {
        List<QuestionOptionInput> options = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            options.add(option(String.valueOf((char) ('A' + i)), i == 0));
        }
        CreateQuestionRequest request = validRequest(QuestionType.SINGLE_CHOICE,
                options.toArray(new QuestionOptionInput[0]));

        assertFieldError(request, QuestionFormValidator.FIELD_OPTIONS,
                QuestionFormValidator.ERROR_OPTIONS_MAX);
    }

    @Test
    public void blankOptionText_fails() {
        CreateQuestionRequest request = validRequest(
                QuestionType.SINGLE_CHOICE,
                option("   ", true),
                option("B", false)
        );

        assertFieldError(request, QuestionFormValidator.optionTextKey(0),
                QuestionFormValidator.ERROR_REQUIRED);
    }

    @Test
    public void optionText2001_fails() {
        CreateQuestionRequest request = validRequest(
                QuestionType.SINGLE_CHOICE,
                option(repeat('a', 2001), true),
                option("B", false)
        );

        assertFieldError(request, QuestionFormValidator.optionTextKey(0),
                QuestionFormValidator.ERROR_TOO_LONG);
    }

    @Test
    public void singleChoiceWithZeroCorrect_fails() {
        assertFormError(validRequest(
                QuestionType.SINGLE_CHOICE,
                option("A", false),
                option("B", false)
        ), QuestionFormValidator.ERROR_SINGLE_CHOICE_CORRECT);
    }

    @Test
    public void singleChoiceWithTwoCorrect_fails() {
        assertFormError(validRequest(
                QuestionType.SINGLE_CHOICE,
                option("A", true),
                option("B", true)
        ), QuestionFormValidator.ERROR_SINGLE_CHOICE_CORRECT);
    }

    @Test
    public void multipleChoiceWithZeroCorrect_fails() {
        assertFormError(validRequest(
                QuestionType.MULTIPLE_CHOICE,
                option("A", false),
                option("B", false)
        ), QuestionFormValidator.ERROR_MULTI_CHOICE_CORRECT);
    }

    @Test
    public void trueFalseWithNotExactlyTwoOptions_fails() {
        assertFormError(validRequest(
                QuestionType.TRUE_FALSE,
                option("Đ", true),
                option("S", false),
                option("C", false)
        ), QuestionFormValidator.ERROR_TRUE_FALSE_SIZE);
    }

    @Test
    public void trueFalseWithZeroCorrect_fails() {
        assertFormError(validRequest(
                QuestionType.TRUE_FALSE,
                option("Đ", false),
                option("S", false)
        ), QuestionFormValidator.ERROR_TRUE_FALSE_CORRECT);
    }

    @Test
    public void trueFalseWithTwoCorrect_fails() {
        assertFormError(validRequest(
                QuestionType.TRUE_FALSE,
                option("Đ", true),
                option("S", true)
        ), QuestionFormValidator.ERROR_TRUE_FALSE_CORRECT);
    }

    private void assertFieldError(CreateQuestionRequest request, String field, String error) {
        QuestionFormValidator.ValidationResult result = validator.validateCreate(request);
        assertFalse(result.isValid());
        assertEquals(error, result.getFieldErrors().get(field));
    }

    private void assertFormError(CreateQuestionRequest request, String error) {
        QuestionFormValidator.ValidationResult result = validator.validateCreate(request);
        assertFalse(result.isValid());
        assertEquals(error, result.getFormError());
    }

    private static CreateQuestionRequest validRequest(QuestionOptionInput... options) {
        return validRequest(QuestionType.SINGLE_CHOICE, options);
    }

    private static CreateQuestionRequest validRequest(
            QuestionType type,
            QuestionOptionInput... options
    ) {
        CreateQuestionRequest request = new CreateQuestionRequest();
        request.setSubjectId(1L);
        request.setTopicId(2L);
        request.setSubtopicId(3L);
        request.setDifficulty(Difficulty.MEDIUM);
        request.setQuestionType(type);
        request.setQuestionText("Question text");
        request.setExplanation("Explanation");
        request.setSource("Source");
        request.setTags("tag1,tag2");
        request.setImageUrl("https://example.com/image.png");
        request.setOptions(options.length == 0
                ? Arrays.asList(option("A", true), option("B", false))
                : Arrays.asList(options));
        return request;
    }

    private static QuestionOptionInput option(String text, boolean correct) {
        QuestionOptionInput option = new QuestionOptionInput();
        option.setOptionLabel(text.length() == 1 ? text : "A");
        option.setOptionText(text);
        option.setIsCorrect(correct);
        option.setDisplayOrder(1);
        return option;
    }

    private static String repeat(char value, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, value);
        return new String(chars);
    }
}
