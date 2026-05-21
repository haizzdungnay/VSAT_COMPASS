package com.example.v_sat_compass.ui.collaborator;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.SubjectResponse;
import com.example.v_sat_compass.data.model.SubtopicResponse;
import com.example.v_sat_compass.data.model.TopicResponse;
import com.example.v_sat_compass.data.model.enums.Difficulty;
import com.example.v_sat_compass.data.model.enums.QuestionType;
import com.example.v_sat_compass.data.model.question.CreateQuestionRequest;
import com.example.v_sat_compass.data.model.question.QuestionOptionInput;
import com.example.v_sat_compass.data.model.question.QuestionResponse;
import com.example.v_sat_compass.data.repository.Resource;
import com.example.v_sat_compass.data.repository.SubjectRepository;
import com.example.v_sat_compass.data.validation.QuestionFormValidator;
import com.example.v_sat_compass.databinding.ActivityCollaboratorCreateQuestionBinding;
import com.example.v_sat_compass.databinding.ItemQuestionOptionRowBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CollaboratorCreateQuestionActivity extends AppCompatActivity {

    private static final int INITIAL_OPTION_COUNT = 4;
    private static final int MIN_OPTIONS = 2;
    private static final int MAX_OPTIONS = 10;

    private ActivityCollaboratorCreateQuestionBinding binding;
    private CollaboratorQuestionViewModel viewModel;
    private SubjectRepository subjectRepository;
    private final QuestionFormValidator validator = new QuestionFormValidator();

    private final List<SubjectResponse> subjects = new ArrayList<>();
    private final List<TopicResponse> topics = new ArrayList<>();
    private final List<SubtopicResponse> subtopics = new ArrayList<>();
    private final List<OptionRow> optionRows = new ArrayList<>();

    private Long selectedSubjectId;
    private Long selectedTopicId;
    private Long selectedSubtopicId;
    private Difficulty selectedDifficulty = Difficulty.MEDIUM;
    private QuestionType selectedQuestionType = QuestionType.SINGLE_CHOICE;
    private boolean submitAfterCreate = false;
    private boolean inFlight = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCollaboratorCreateQuestionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CollaboratorQuestionViewModel.class);
        subjectRepository = new SubjectRepository();

        binding.btnBack.setOnClickListener(v -> finish());
        setupStaticDropdowns();
        setupActions();
        observeViewModel();
        renderOptionEditor();
        loadSubjects();
    }

    private void setupStaticDropdowns() {
        List<DropdownItem<Difficulty>> difficultyItems = Arrays.asList(
                new DropdownItem<>(getString(R.string.cq_difficulty_easy), Difficulty.EASY),
                new DropdownItem<>(getString(R.string.cq_difficulty_medium), Difficulty.MEDIUM),
                new DropdownItem<>(getString(R.string.cq_difficulty_hard), Difficulty.HARD),
                new DropdownItem<>(getString(R.string.cq_difficulty_very_hard), Difficulty.VERY_HARD)
        );
        binding.dropdownDifficulty.setAdapter(labelAdapter(difficultyItems));
        binding.dropdownDifficulty.setText(labelFor(difficultyItems, selectedDifficulty), false);
        binding.dropdownDifficulty.setOnItemClickListener((parent, view, position, id) -> {
            selectedDifficulty = difficultyItems.get(position).value;
            binding.layoutDifficulty.setError(null);
        });

        List<DropdownItem<QuestionType>> typeItems = Arrays.asList(
                new DropdownItem<>(getString(R.string.cq_type_single_choice),
                        QuestionType.SINGLE_CHOICE),
                new DropdownItem<>(getString(R.string.cq_type_multiple_choice),
                        QuestionType.MULTIPLE_CHOICE),
                new DropdownItem<>(getString(R.string.cq_type_true_false),
                        QuestionType.TRUE_FALSE)
        );
        binding.dropdownQuestionType.setAdapter(labelAdapter(typeItems));
        binding.dropdownQuestionType.setText(labelFor(typeItems, selectedQuestionType), false);
        binding.dropdownQuestionType.setOnItemClickListener((parent, view, position, id) -> {
            selectedQuestionType = typeItems.get(position).value;
            binding.layoutQuestionType.setError(null);
            renderOptionEditor();
        });
    }

    private void setupActions() {
        binding.btnAddOption.setOnClickListener(v -> addOptionRow(null));
        binding.btnSaveDraft.setOnClickListener(v -> save(false));
        binding.btnSubmitReview.setOnClickListener(v -> save(true));
    }

    private void observeViewModel() {
        viewModel.getTopicListState().observe(this, resource -> {
            if (resource == null) {
                return;
            }
            if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null) {
                populateTopics(resource.getData());
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                Toast.makeText(this, messageOrGeneric(resource.getMessage()), Toast.LENGTH_SHORT)
                        .show();
            }
        });

        viewModel.getSubtopicListState().observe(this, resource -> {
            if (resource == null) {
                return;
            }
            if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null) {
                populateSubtopics(resource.getData());
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                Toast.makeText(this, messageOrGeneric(resource.getMessage()), Toast.LENGTH_SHORT)
                        .show();
            }
        });

        viewModel.getCreateQuestionState().observe(this, resource -> {
            if (resource == null) {
                return;
            }
            if (resource.getStatus() == Resource.Status.LOADING) {
                setInFlight(true);
            } else if (resource.getStatus() == Resource.Status.SUCCESS) {
                handleCreateSuccess(resource.getData());
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                submitAfterCreate = false;
                setInFlight(false);
                Toast.makeText(this, messageOrGeneric(resource.getMessage()), Toast.LENGTH_SHORT)
                        .show();
            }
        });

        viewModel.getSubmitForReviewState().observe(this, resource -> {
            if (resource == null) {
                return;
            }
            if (resource.getStatus() == Resource.Status.LOADING) {
                setInFlight(true);
            } else if (resource.getStatus() == Resource.Status.SUCCESS) {
                Toast.makeText(this, R.string.ccq_success_submit, Toast.LENGTH_SHORT).show();
                finish();
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                setInFlight(false);
                Toast.makeText(this, messageOrGeneric(resource.getMessage()), Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private void loadSubjects() {
        subjectRepository.getSubjects(new SubjectRepository.SubjectCallback() {
            @Override
            public void onSuccess(List<SubjectResponse> data) {
                populateSubjects(data);
            }

            @Override
            public void onError(SubjectRepository.SubjectError error) {
                Toast.makeText(
                        CollaboratorCreateQuestionActivity.this,
                        error != null && error.getMessage() != null
                                ? error.getMessage()
                                : getString(R.string.ccq_error_generic),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void populateSubjects(List<SubjectResponse> data) {
        subjects.clear();
        if (data != null) {
            subjects.addAll(data);
        }

        List<DropdownItem<SubjectResponse>> items = new ArrayList<>();
        for (SubjectResponse subject : subjects) {
            items.add(new DropdownItem<>(displayName(subject.getName(), subject.getCode()), subject));
        }
        binding.dropdownSubject.setAdapter(labelAdapter(items));
        binding.dropdownSubject.setOnItemClickListener((parent, view, position, id) -> {
            SubjectResponse subject = items.get(position).value;
            selectedSubjectId = subject.getId();
            binding.layoutSubject.setError(null);
            resetTopicSelection();
            resetSubtopicSelection();
            if (selectedSubjectId != null) {
                viewModel.listTopics(selectedSubjectId);
            }
        });
    }

    private void populateTopics(List<TopicResponse> data) {
        topics.clear();
        if (data != null) {
            topics.addAll(data);
        }

        List<DropdownItem<TopicResponse>> items = new ArrayList<>();
        for (TopicResponse topic : topics) {
            items.add(new DropdownItem<>(displayName(topic.getName(), topic.getCode()), topic));
        }
        binding.dropdownTopic.setAdapter(labelAdapter(items));
        binding.dropdownTopic.setOnItemClickListener((parent, view, position, id) -> {
            TopicResponse topic = items.get(position).value;
            selectedTopicId = topic.getId();
            binding.layoutTopic.setError(null);
            resetSubtopicSelection();
            if (selectedSubjectId != null && selectedTopicId != null) {
                viewModel.listSubtopics(selectedSubjectId, selectedTopicId);
            }
        });
    }

    private void populateSubtopics(List<SubtopicResponse> data) {
        subtopics.clear();
        if (data != null) {
            subtopics.addAll(data);
        }

        List<DropdownItem<SubtopicResponse>> items = new ArrayList<>();
        for (SubtopicResponse subtopic : subtopics) {
            items.add(new DropdownItem<>(displayName(subtopic.getName(), subtopic.getCode()), subtopic));
        }
        binding.dropdownSubtopic.setAdapter(labelAdapter(items));
        binding.dropdownSubtopic.setOnItemClickListener((parent, view, position, id) -> {
            selectedSubtopicId = items.get(position).value.getId();
        });
    }

    private void resetTopicSelection() {
        selectedTopicId = null;
        topics.clear();
        binding.dropdownTopic.setText("", false);
        binding.dropdownTopic.setAdapter(labelAdapter(new ArrayList<>()));
        binding.layoutTopic.setError(null);
    }

    private void resetSubtopicSelection() {
        selectedSubtopicId = null;
        subtopics.clear();
        binding.dropdownSubtopic.setText("", false);
        binding.dropdownSubtopic.setAdapter(labelAdapter(new ArrayList<>()));
    }

    private <T> ArrayAdapter<String> labelAdapter(List<DropdownItem<T>> items) {
        List<String> labels = new ArrayList<>();
        for (DropdownItem<T> item : items) {
            labels.add(item.label);
        }
        return new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, labels);
    }

    private <T> String labelFor(List<DropdownItem<T>> items, T value) {
        for (DropdownItem<T> item : items) {
            if (item.value == value || item.value.equals(value)) {
                return item.label;
            }
        }
        return "";
    }

    private void renderOptionEditor() {
        binding.containerOptions.removeAllViews();
        optionRows.clear();
        if (selectedQuestionType == QuestionType.TRUE_FALSE) {
            addOptionRow(getString(R.string.ccq_true_label));
            addOptionRow(getString(R.string.ccq_false_label));
            setCorrectIndex(0);
        } else {
            for (int i = 0; i < INITIAL_OPTION_COUNT; i++) {
                addOptionRow(null);
            }
            if (selectedQuestionType == QuestionType.SINGLE_CHOICE) {
                setCorrectIndex(0);
            }
        }
        updateOptionControls();
    }

    private void addOptionRow(String presetText) {
        if (selectedQuestionType != QuestionType.TRUE_FALSE && optionRows.size() >= MAX_OPTIONS) {
            return;
        }

        ItemQuestionOptionRowBinding rowBinding =
                ItemQuestionOptionRowBinding.inflate(getLayoutInflater(), binding.containerOptions, false);
        OptionRow row = new OptionRow(rowBinding);
        if (presetText != null) {
            rowBinding.inputOptionText.setText(presetText);
            rowBinding.inputOptionText.setEnabled(false);
        }
        rowBinding.radioCorrect.setOnClickListener(v -> setCorrectIndex(optionRows.indexOf(row)));
        rowBinding.btnRemoveOption.setOnClickListener(v -> removeOptionRow(row));

        optionRows.add(row);
        binding.containerOptions.addView(rowBinding.getRoot());
        updateOptionControls();
    }

    private void removeOptionRow(OptionRow row) {
        if (selectedQuestionType == QuestionType.TRUE_FALSE || optionRows.size() <= MIN_OPTIONS) {
            return;
        }
        binding.containerOptions.removeView(row.binding.getRoot());
        optionRows.remove(row);
        if (selectedQuestionType == QuestionType.SINGLE_CHOICE && selectedCorrectCount() == 0
                && !optionRows.isEmpty()) {
            optionRows.get(0).binding.radioCorrect.setChecked(true);
        }
        updateOptionControls();
    }

    private void setCorrectIndex(int index) {
        if (index < 0 || index >= optionRows.size()) {
            return;
        }
        for (int i = 0; i < optionRows.size(); i++) {
            optionRows.get(i).binding.radioCorrect.setChecked(i == index);
        }
    }

    private void updateOptionControls() {
        boolean trueFalse = selectedQuestionType == QuestionType.TRUE_FALSE;
        boolean multipleChoice = selectedQuestionType == QuestionType.MULTIPLE_CHOICE;
        for (int i = 0; i < optionRows.size(); i++) {
            OptionRow row = optionRows.get(i);
            row.binding.textOptionLabel.setText(optionLabel(i, trueFalse));
            row.binding.radioCorrect.setVisibility(multipleChoice ? View.GONE : View.VISIBLE);
            row.binding.checkboxCorrect.setVisibility(multipleChoice ? View.VISIBLE : View.GONE);
            row.binding.btnRemoveOption.setVisibility(
                    !trueFalse && optionRows.size() > MIN_OPTIONS ? View.VISIBLE : View.INVISIBLE
            );
        }
        binding.btnAddOption.setVisibility(trueFalse ? View.GONE : View.VISIBLE);
        binding.btnAddOption.setEnabled(!trueFalse && optionRows.size() < MAX_OPTIONS);
    }

    private int selectedCorrectCount() {
        int count = 0;
        for (OptionRow row : optionRows) {
            if (row.binding.radioCorrect.isChecked()
                    || row.binding.checkboxCorrect.isChecked()) {
                count++;
            }
        }
        return count;
    }

    private void save(boolean submit) {
        if (inFlight) {
            return;
        }

        clearErrors();
        CreateQuestionRequest request = collectRequest();
        QuestionFormValidator.ValidationResult result = validator.validateCreate(request);
        if (!result.isValid()) {
            applyValidationErrors(result);
            return;
        }

        submitAfterCreate = submit;
        viewModel.createQuestion(request);
    }

    private CreateQuestionRequest collectRequest() {
        return new CreateQuestionRequest(
                selectedSubjectId,
                selectedTopicId,
                selectedSubtopicId,
                selectedDifficulty,
                selectedQuestionType,
                textOf(binding.inputQuestionText),
                null,
                emptyToNull(textOf(binding.inputImageUrl)),
                emptyToNull(textOf(binding.inputExplanation)),
                null,
                emptyToNull(textOf(binding.inputSource)),
                emptyToNull(textOf(binding.inputTags)),
                collectOptions()
        );
    }

    private List<QuestionOptionInput> collectOptions() {
        List<QuestionOptionInput> options = new ArrayList<>();
        boolean trueFalse = selectedQuestionType == QuestionType.TRUE_FALSE;
        for (int i = 0; i < optionRows.size(); i++) {
            OptionRow row = optionRows.get(i);
            boolean correct = selectedQuestionType == QuestionType.MULTIPLE_CHOICE
                    ? row.binding.checkboxCorrect.isChecked()
                    : row.binding.radioCorrect.isChecked();
            options.add(new QuestionOptionInput(
                    optionLabel(i, trueFalse),
                    textOf(row.binding.inputOptionText),
                    null,
                    null,
                    correct,
                    i + 1
            ));
        }
        return options;
    }

    private void applyValidationErrors(QuestionFormValidator.ValidationResult result) {
        Map<String, String> errors = result.getFieldErrors();
        binding.layoutSubject.setError(resolveRequired(errors.get(
                QuestionFormValidator.FIELD_SUBJECT_ID), R.string.ccq_validation_subject_required));
        binding.layoutTopic.setError(resolveRequired(errors.get(
                QuestionFormValidator.FIELD_TOPIC_ID), R.string.ccq_validation_topic_required));
        binding.layoutDifficulty.setError(resolveRequired(errors.get(
                QuestionFormValidator.FIELD_DIFFICULTY), R.string.ccq_validation_difficulty_required));
        binding.layoutQuestionType.setError(resolveRequired(errors.get(
                QuestionFormValidator.FIELD_QUESTION_TYPE), R.string.ccq_validation_type_required));
        binding.layoutQuestionText.setError(resolveQuestionTextError(errors.get(
                QuestionFormValidator.FIELD_QUESTION_TEXT)));
        binding.layoutExplanation.setError(resolveTooLongOnly(errors.get(
                QuestionFormValidator.FIELD_EXPLANATION)));
        binding.layoutSource.setError(resolveLengthError(errors.get(
                QuestionFormValidator.FIELD_SOURCE)));
        binding.layoutTags.setError(resolveLengthError(errors.get(
                QuestionFormValidator.FIELD_TAGS)));
        binding.layoutImageUrl.setError(resolveLengthError(errors.get(
                QuestionFormValidator.FIELD_IMAGE_URL)));

        for (int i = 0; i < optionRows.size(); i++) {
            String key = QuestionFormValidator.optionTextKey(i);
            optionRows.get(i).binding.layoutOptionText.setError(
                    resolveOptionTextError(errors.get(key))
            );
        }

        String formError = result.getFormError();
        if (formError != null) {
            Toast.makeText(this, resolveFormError(formError), Toast.LENGTH_SHORT).show();
        } else if (errors.containsKey(QuestionFormValidator.FIELD_OPTIONS)) {
            Toast.makeText(this, resolveOptionsError(
                    errors.get(QuestionFormValidator.FIELD_OPTIONS)), Toast.LENGTH_SHORT).show();
        }
    }

    private void clearErrors() {
        binding.layoutSubject.setError(null);
        binding.layoutTopic.setError(null);
        binding.layoutDifficulty.setError(null);
        binding.layoutQuestionType.setError(null);
        binding.layoutQuestionText.setError(null);
        binding.layoutExplanation.setError(null);
        binding.layoutSource.setError(null);
        binding.layoutTags.setError(null);
        binding.layoutImageUrl.setError(null);
        for (OptionRow row : optionRows) {
            row.binding.layoutOptionText.setError(null);
        }
    }

    private void handleCreateSuccess(QuestionResponse response) {
        if (!submitAfterCreate) {
            Toast.makeText(this, R.string.ccq_success_draft, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (response == null || response.getId() == null) {
            submitAfterCreate = false;
            setInFlight(false);
            Toast.makeText(this, R.string.ccq_error_generic, Toast.LENGTH_SHORT).show();
            return;
        }

        submitAfterCreate = false;
        viewModel.submitForReview(response.getId());
    }

    private void setInFlight(boolean loading) {
        inFlight = loading;
        binding.progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSaveDraft.setEnabled(!loading);
        binding.btnSubmitReview.setEnabled(!loading);
    }

    private String resolveRequired(String error, int stringRes) {
        return QuestionFormValidator.ERROR_REQUIRED.equals(error)
                ? getString(stringRes)
                : null;
    }

    private String resolveQuestionTextError(String error) {
        if (QuestionFormValidator.ERROR_REQUIRED.equals(error)) {
            return getString(R.string.ccq_validation_question_text_required);
        }
        if (QuestionFormValidator.ERROR_TOO_LONG.equals(error)) {
            return getString(R.string.ccq_validation_question_text_too_long);
        }
        return null;
    }

    private String resolveOptionTextError(String error) {
        if (QuestionFormValidator.ERROR_REQUIRED.equals(error)) {
            return getString(R.string.ccq_validation_option_text_required);
        }
        if (QuestionFormValidator.ERROR_TOO_LONG.equals(error)) {
            return getString(R.string.ccq_validation_option_text_too_long);
        }
        return null;
    }

    private String resolveTooLongOnly(String error) {
        return QuestionFormValidator.ERROR_TOO_LONG.equals(error)
                ? getString(R.string.ccq_error_generic)
                : null;
    }

    private String resolveLengthError(String error) {
        return QuestionFormValidator.ERROR_TOO_LONG.equals(error)
                ? getString(R.string.ccq_error_generic)
                : null;
    }

    private String resolveOptionsError(String error) {
        if (QuestionFormValidator.ERROR_OPTIONS_MIN.equals(error)) {
            return getString(R.string.ccq_validation_options_min);
        }
        if (QuestionFormValidator.ERROR_OPTIONS_MAX.equals(error)) {
            return getString(R.string.ccq_validation_options_max);
        }
        return getString(R.string.ccq_error_generic);
    }

    private String resolveFormError(String error) {
        if (QuestionFormValidator.ERROR_SINGLE_CHOICE_CORRECT.equals(error)) {
            return getString(R.string.ccq_validation_single_choice_correct);
        }
        if (QuestionFormValidator.ERROR_MULTI_CHOICE_CORRECT.equals(error)) {
            return getString(R.string.ccq_validation_multi_choice_correct);
        }
        if (QuestionFormValidator.ERROR_TRUE_FALSE_SIZE.equals(error)) {
            return getString(R.string.ccq_validation_true_false_correct);
        }
        if (QuestionFormValidator.ERROR_TRUE_FALSE_CORRECT.equals(error)) {
            return getString(R.string.ccq_validation_true_false_correct);
        }
        return getString(R.string.ccq_error_generic);
    }

    private String messageOrGeneric(String message) {
        return message != null && !message.trim().isEmpty()
                ? message
                : getString(R.string.ccq_error_generic);
    }

    private static String textOf(android.widget.TextView view) {
        return view.getText() != null ? view.getText().toString().trim() : "";
    }

    private static String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value;
    }

    private static String displayName(String name, String code) {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        return code != null ? code : "";
    }

    private static String optionLabel(int index, boolean trueFalse) {
        if (trueFalse) {
            return index == 0 ? "Đ" : "S";
        }
        return String.valueOf((char) ('A' + index));
    }

    private static final class DropdownItem<T> {
        final String label;
        final T value;

        DropdownItem(String label, T value) {
            this.label = label;
            this.value = value;
        }
    }

    private static final class OptionRow {
        final ItemQuestionOptionRowBinding binding;

        OptionRow(ItemQuestionOptionRowBinding binding) {
            this.binding = binding;
        }
    }
}
