package com.example.v_sat_compass.ui.collaborator;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.SubjectResponse;
import com.example.v_sat_compass.data.model.SubtopicResponse;
import com.example.v_sat_compass.data.model.TopicResponse;
import com.example.v_sat_compass.data.model.enums.Difficulty;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.enums.QuestionType;
import com.example.v_sat_compass.data.model.enums.ReviewAction;
import com.example.v_sat_compass.data.model.question.QuestionOptionInput;
import com.example.v_sat_compass.data.model.question.QuestionOptionResponse;
import com.example.v_sat_compass.data.model.question.QuestionResponse;
import com.example.v_sat_compass.data.model.question.QuestionReviewResponse;
import com.example.v_sat_compass.data.model.question.UpdateQuestionRequest;
import com.example.v_sat_compass.data.repository.Resource;
import com.example.v_sat_compass.data.repository.SubjectRepository;
import com.example.v_sat_compass.data.validation.QuestionFormValidator;
import com.example.v_sat_compass.databinding.ActivityCollaboratorQuestionDetailBinding;
import com.example.v_sat_compass.databinding.ItemQuestionOptionRowBinding;
import com.example.v_sat_compass.databinding.ItemQuestionOptionViewBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CollaboratorQuestionDetailActivity extends AppCompatActivity {

    public static final String EXTRA_QUESTION_ID = "questionId";

    private static final int INITIAL_OPTION_COUNT = 4;
    private static final int MIN_OPTIONS = 2;
    private static final int MAX_OPTIONS = 10;

    private ActivityCollaboratorQuestionDetailBinding binding;
    private CollaboratorQuestionViewModel viewModel;
    private SubjectRepository subjectRepository;
    private final QuestionFormValidator validator = new QuestionFormValidator();

    private final List<SubjectResponse> subjects = new ArrayList<>();
    private final List<TopicResponse> topics = new ArrayList<>();
    private final List<SubtopicResponse> subtopics = new ArrayList<>();
    private final List<OptionRow> optionRows = new ArrayList<>();

    private List<DropdownItem<Difficulty>> difficultyItems;
    private List<DropdownItem<QuestionType>> typeItems;
    private Long questionId;
    private QuestionResponse currentQuestion;
    private Long selectedSubjectId;
    private Long selectedTopicId;
    private Long selectedSubtopicId;
    private Difficulty selectedDifficulty;
    private QuestionType selectedQuestionType;
    private boolean inFlight;
    private boolean editMode;
    private boolean cascadeError;
    private boolean pendingTopicSelection;
    private boolean pendingSubtopicSelection;
    private boolean reviewHistoryExpanded = true;

    static boolean isEditableStatus(QuestionStatus status) {
        return status == QuestionStatus.DRAFT || status == QuestionStatus.NEEDS_REVISION;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCollaboratorQuestionDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        long extraId = getIntent().getLongExtra(EXTRA_QUESTION_ID, -1L);
        if (extraId <= 0L) {
            Toast.makeText(this, R.string.cqd_error_missing_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        questionId = extraId;

        viewModel = new ViewModelProvider(this).get(CollaboratorQuestionViewModel.class);
        subjectRepository = new SubjectRepository();

        binding.btnBack.setOnClickListener(v -> finish());
        setupStaticDropdowns();
        setupActions();
        observeViewModel();
        loadSubjects();
        viewModel.getQuestion(questionId);
    }

    private void setupStaticDropdowns() {
        difficultyItems = Arrays.asList(
                new DropdownItem<>(getString(R.string.cq_difficulty_easy), Difficulty.EASY),
                new DropdownItem<>(getString(R.string.cq_difficulty_medium), Difficulty.MEDIUM),
                new DropdownItem<>(getString(R.string.cq_difficulty_hard), Difficulty.HARD),
                new DropdownItem<>(getString(R.string.cq_difficulty_very_hard), Difficulty.VERY_HARD)
        );
        binding.dropdownDifficulty.setAdapter(labelAdapter(difficultyItems));
        binding.dropdownDifficulty.setOnItemClickListener((parent, view, position, id) -> {
            selectedDifficulty = difficultyItems.get(position).value;
            binding.layoutDifficulty.setError(null);
        });

        typeItems = Arrays.asList(
                new DropdownItem<>(getString(R.string.cq_type_single_choice),
                        QuestionType.SINGLE_CHOICE),
                new DropdownItem<>(getString(R.string.cq_type_multiple_choice),
                        QuestionType.MULTIPLE_CHOICE),
                new DropdownItem<>(getString(R.string.cq_type_true_false),
                        QuestionType.TRUE_FALSE)
        );
        binding.dropdownQuestionType.setAdapter(labelAdapter(typeItems));
        binding.dropdownQuestionType.setOnItemClickListener((parent, view, position, id) -> {
            QuestionType nextType = typeItems.get(position).value;
            if (selectedQuestionType != nextType) {
                selectedQuestionType = nextType;
                renderDefaultOptionEditor();
            }
            binding.layoutQuestionType.setError(null);
        });
    }

    private void setupActions() {
        binding.btnRetry.setOnClickListener(v -> viewModel.getQuestion(questionId));
        binding.btnEdit.setOnClickListener(v -> enterEditMode());
        binding.btnSubmitReview.setOnClickListener(v -> confirmSubmitForReview());
        binding.cardReviewHistory.setOnClickListener(v -> toggleReviewHistory());
        binding.btnAddOption.setOnClickListener(v -> addOptionRow(null, false, false));
        binding.btnSave.setOnClickListener(v -> saveChanges());
        binding.btnCancel.setOnClickListener(v -> exitEditMode());
    }

    private void observeViewModel() {
        viewModel.getQuestionState().observe(this, resource -> {
            if (resource == null) {
                return;
            }
            if (resource.getStatus() == Resource.Status.LOADING) {
                showLoading();
            } else if (resource.getStatus() == Resource.Status.SUCCESS) {
                currentQuestion = resource.getData();
                setInFlight(false);
                renderViewMode();
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                setInFlight(false);
                showError(resource.getMessage());
            }
        });

        viewModel.getUpdateQuestionState().observe(this, resource -> {
            if (resource == null) {
                return;
            }
            if (resource.getStatus() == Resource.Status.LOADING) {
                setInFlight(true);
            } else if (resource.getStatus() == Resource.Status.SUCCESS) {
                setInFlight(false);
                currentQuestion = resource.getData();
                Toast.makeText(this, R.string.cqd_success_update, Toast.LENGTH_SHORT).show();
                renderViewMode();
            } else if (resource.getStatus() == Resource.Status.ERROR) {
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
                Toast.makeText(this, R.string.cqd_success_submit, Toast.LENGTH_SHORT).show();
                viewModel.getQuestion(questionId);
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                setInFlight(false);
                Toast.makeText(this, messageOrGeneric(resource.getMessage()), Toast.LENGTH_SHORT)
                        .show();
            }
        });

        viewModel.getTopicListState().observe(this, resource -> {
            if (resource == null) {
                return;
            }
            if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null) {
                cascadeError = false;
                populateTopics(resource.getData());
                updateActionEnabled();
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                cascadeError = true;
                updateActionEnabled();
                Toast.makeText(this, messageOrGeneric(resource.getMessage()), Toast.LENGTH_SHORT)
                        .show();
            }
        });

        viewModel.getSubtopicListState().observe(this, resource -> {
            if (resource == null) {
                return;
            }
            if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null) {
                cascadeError = false;
                populateSubtopics(resource.getData());
                updateActionEnabled();
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                cascadeError = true;
                updateActionEnabled();
                Toast.makeText(this, messageOrGeneric(resource.getMessage()), Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private void showLoading() {
        binding.progressLoading.setVisibility(View.VISIBLE);
        binding.errorContainer.setVisibility(View.GONE);
        binding.viewContainer.setVisibility(View.GONE);
        binding.editContainer.setVisibility(View.GONE);
    }

    private void showError(String message) {
        binding.progressLoading.setVisibility(View.GONE);
        binding.viewContainer.setVisibility(View.GONE);
        binding.editContainer.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.VISIBLE);
        binding.textErrorMessage.setText(message != null && !message.trim().isEmpty()
                ? message
                : getString(R.string.cqd_error_load));
    }

    private void renderViewMode() {
        editMode = false;
        binding.progressLoading.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.GONE);
        binding.editContainer.setVisibility(View.GONE);
        binding.viewContainer.setVisibility(View.VISIBLE);

        QuestionResponse question = currentQuestion;
        if (question == null) {
            showError(getString(R.string.cqd_error_load));
            return;
        }

        binding.textQuestionCode.setText(nonEmpty(question.getQuestionCode(), ""));
        bindEnumBadge(binding.badgeStatus,
                CollaboratorQuestionListAdapter.statusLabelRes(question.getStatus()),
                question.getStatus(),
                CollaboratorQuestionListAdapter.statusColorRes(question.getStatus()));
        bindEnumBadge(binding.badgeDifficulty,
                CollaboratorQuestionListAdapter.difficultyLabelRes(question.getDifficulty()),
                question.getDifficulty(),
                CollaboratorQuestionListAdapter.difficultyColorRes(question.getDifficulty()));
        bindEnumText(binding.badgeQuestionType,
                CollaboratorQuestionListAdapter.questionTypeLabelRes(question.getQuestionType()),
                question.getQuestionType());

        bindIdText(binding.textSubject, question.getSubjectId(), R.string.cqd_label_subject_id);
        bindIdText(binding.textTopic, question.getTopicId(), R.string.cqd_label_topic_id);
        bindOptionalIdText(binding.textSubtopic, question.getSubtopicId(),
                R.string.cqd_label_subtopic_id);
        binding.textQuestionText.setText(nonEmpty(question.getQuestionText(), ""));
        renderOptionViews(question.getOptions());
        bindOptionalSection(binding.sectionExplanation, binding.textExplanation,
                question.getExplanation());
        bindOptionalFormattedText(binding.textSource, question.getSource(),
                R.string.cqd_label_source);
        bindOptionalFormattedText(binding.textTags, question.getTags(), R.string.cqd_label_tags);
        bindOptionalFormattedText(binding.textImageUrl, question.getImageUrl(),
                R.string.cqd_label_image_url);
        binding.textCreatedAt.setText(getString(R.string.cqd_label_created_at,
                formatDate(question.getCreatedAt())));
        binding.textUpdatedAt.setText(getString(R.string.cqd_label_updated_at,
                formatDate(question.getUpdatedAt())));
        binding.textVersion.setText(getString(R.string.cqd_label_version,
                question.getVersion() != null ? question.getVersion() : 0));
        renderReviewHistory(question.getReviewHistory());
        renderActions(question.getStatus());
    }

    private void renderActions(QuestionStatus status) {
        boolean editable = isEditableStatus(status);
        binding.actionContainer.setVisibility(editable ? View.VISIBLE : View.GONE);
        binding.textReadOnlyBanner.setVisibility(editable ? View.GONE : View.VISIBLE);
        if (!editable) {
            int bannerRes = statusBannerRes(status);
            binding.textReadOnlyBanner.setText(bannerRes != 0 ? getString(bannerRes) : "");
        }
    }

    private void enterEditMode() {
        if (currentQuestion == null || !isEditableStatus(currentQuestion.getStatus())) {
            return;
        }
        editMode = true;
        cascadeError = false;
        binding.progressLoading.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.GONE);
        binding.viewContainer.setVisibility(View.GONE);
        binding.editContainer.setVisibility(View.VISIBLE);
        populateEditForm(currentQuestion);
        updateActionEnabled();
    }

    private void exitEditMode() {
        clearErrors();
        renderViewMode();
    }

    private void populateEditForm(QuestionResponse question) {
        selectedSubjectId = question.getSubjectId();
        selectedTopicId = question.getTopicId();
        selectedSubtopicId = question.getSubtopicId();
        selectedDifficulty = question.getDifficulty();
        selectedQuestionType = question.getQuestionType() != null
                ? question.getQuestionType()
                : QuestionType.SINGLE_CHOICE;
        pendingTopicSelection = selectedTopicId != null;
        pendingSubtopicSelection = selectedSubtopicId != null;

        binding.dropdownDifficulty.setText(labelFor(difficultyItems, selectedDifficulty), false);
        binding.dropdownQuestionType.setText(labelFor(typeItems, selectedQuestionType), false);
        binding.inputQuestionText.setText(nonEmpty(question.getQuestionText(), ""));
        binding.inputExplanation.setText(nonEmpty(question.getExplanation(), ""));
        binding.inputSource.setText(nonEmpty(question.getSource(), ""));
        binding.inputTags.setText(nonEmpty(question.getTags(), ""));
        binding.inputImageUrl.setText(nonEmpty(question.getImageUrl(), ""));
        renderOptionEditorFromResponse(question.getOptions());
        bindSubjectSelection();
        resetTopicSelection(false);
        resetSubtopicSelection(false);
        if (selectedSubjectId != null) {
            viewModel.listTopics(selectedSubjectId);
        }
    }

    private void saveChanges() {
        if (inFlight) {
            return;
        }
        clearErrors();
        UpdateQuestionRequest request = collectUpdateRequest();
        QuestionFormValidator.ValidationResult result = validator.validateUpdate(request);
        if (!result.isValid()) {
            applyValidationErrors(result);
            return;
        }
        viewModel.updateQuestion(questionId, request);
    }

    private UpdateQuestionRequest collectUpdateRequest() {
        return new UpdateQuestionRequest(
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

    private void confirmSubmitForReview() {
        if (currentQuestion == null || !isEditableStatus(currentQuestion.getStatus())) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.cqd_confirm_submit_title)
                .setMessage(R.string.cqd_confirm_submit_message)
                .setPositiveButton(R.string.cqd_confirm_ok,
                        (dialog, which) -> viewModel.submitForReview(questionId))
                .setNegativeButton(R.string.cqd_confirm_cancel, null)
                .show();
    }

    private void renderOptionViews(List<QuestionOptionResponse> options) {
        binding.containerOptionViews.removeAllViews();
        if (options == null) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (QuestionOptionResponse option : options) {
            ItemQuestionOptionViewBinding rowBinding = ItemQuestionOptionViewBinding.inflate(
                    inflater,
                    binding.containerOptionViews,
                    false
            );
            rowBinding.textOptionLabel.setText(nonEmpty(option.getOptionLabel(), ""));
            rowBinding.textOptionText.setText(nonEmpty(option.getOptionText(), ""));
            rowBinding.badgeCorrect.setVisibility(Boolean.TRUE.equals(option.getIsCorrect())
                    ? View.VISIBLE
                    : View.GONE);
            binding.containerOptionViews.addView(rowBinding.getRoot());
        }
    }

    private void renderReviewHistory(List<QuestionReviewResponse> reviews) {
        binding.containerReviewHistory.removeAllViews();
        boolean visible = reviews != null && !reviews.isEmpty();
        binding.cardReviewHistory.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) {
            return;
        }
        binding.reviewHistoryBody.setVisibility(reviewHistoryExpanded ? View.VISIBLE : View.GONE);
        for (QuestionReviewResponse review : reviews) {
            binding.containerReviewHistory.addView(reviewRow(review));
        }
    }

    private View reviewRow(QuestionReviewResponse review) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 8, 0, 8);

        TextView action = new TextView(this);
        action.setText(reviewActionLabel(review.getAction()));
        action.setTextColor(ContextCompat.getColor(this, R.color.white));
        action.setTextSize(12);
        action.setTypeface(action.getTypeface(), android.graphics.Typeface.BOLD);
        action.setPadding(8, 4, 8, 4);
        action.setBackgroundColor(ContextCompat.getColor(this, reviewActionColor(review.getAction())));
        row.addView(action);

        TextView reviewer = detailText(getString(R.string.cqd_label_reviewer_id,
                review.getReviewerId() != null ? review.getReviewerId() : 0L));
        reviewer.setPadding(0, 6, 0, 0);
        row.addView(reviewer);

        if (!isBlank(review.getComment())) {
            row.addView(detailText(review.getComment()));
        }
        row.addView(detailText(getString(R.string.cqd_label_version_reviewed,
                review.getVersionReviewed() != null ? review.getVersionReviewed() : 0)));
        row.addView(detailText(formatDate(review.getCreatedAt())));
        return row;
    }

    private TextView detailText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        view.setTextSize(14);
        return view;
    }

    private void toggleReviewHistory() {
        reviewHistoryExpanded = !reviewHistoryExpanded;
        binding.reviewHistoryBody.setVisibility(reviewHistoryExpanded ? View.VISIBLE : View.GONE);
    }

    private void loadSubjects() {
        subjectRepository.getSubjects(new SubjectRepository.SubjectCallback() {
            @Override
            public void onSuccess(List<SubjectResponse> data) {
                populateSubjects(data);
            }

            @Override
            public void onError(SubjectRepository.SubjectError error) {
                cascadeError = true;
                updateActionEnabled();
                Toast.makeText(
                        CollaboratorQuestionDetailActivity.this,
                        error != null && error.getMessage() != null
                                ? error.getMessage()
                                : getString(R.string.cqd_error_generic),
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
            selectedSubjectId = items.get(position).value.getId();
            pendingTopicSelection = false;
            pendingSubtopicSelection = false;
            binding.layoutSubject.setError(null);
            resetTopicSelection(true);
            resetSubtopicSelection(true);
            if (selectedSubjectId != null) {
                viewModel.listTopics(selectedSubjectId);
            }
        });
        if (editMode && selectedSubjectId != null) {
            bindSubjectSelection();
        }
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
            selectedTopicId = items.get(position).value.getId();
            pendingSubtopicSelection = false;
            binding.layoutTopic.setError(null);
            resetSubtopicSelection(true);
            if (selectedSubjectId != null && selectedTopicId != null) {
                viewModel.listSubtopics(selectedSubjectId, selectedTopicId);
            }
        });
        if (pendingTopicSelection) {
            bindTopicSelection();
            pendingTopicSelection = false;
            if (selectedSubjectId != null && selectedTopicId != null) {
                viewModel.listSubtopics(selectedSubjectId, selectedTopicId);
            }
        }
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
        if (pendingSubtopicSelection) {
            bindSubtopicSelection();
            pendingSubtopicSelection = false;
        }
    }

    private void bindSubjectSelection() {
        binding.dropdownSubject.setText(labelForSubject(selectedSubjectId), false);
    }

    private void bindTopicSelection() {
        binding.dropdownTopic.setText(labelForTopic(selectedTopicId), false);
    }

    private void bindSubtopicSelection() {
        binding.dropdownSubtopic.setText(labelForSubtopic(selectedSubtopicId), false);
    }

    private void resetTopicSelection(boolean clearSelected) {
        if (clearSelected) {
            selectedTopicId = null;
        }
        topics.clear();
        binding.dropdownTopic.setText("", false);
        binding.dropdownTopic.setAdapter(labelAdapter(new ArrayList<>()));
        binding.layoutTopic.setError(null);
    }

    private void resetSubtopicSelection(boolean clearSelected) {
        if (clearSelected) {
            selectedSubtopicId = null;
        }
        subtopics.clear();
        binding.dropdownSubtopic.setText("", false);
        binding.dropdownSubtopic.setAdapter(labelAdapter(new ArrayList<>()));
    }

    private void renderDefaultOptionEditor() {
        binding.containerOptions.removeAllViews();
        optionRows.clear();
        if (selectedQuestionType == QuestionType.TRUE_FALSE) {
            addOptionRow(getString(R.string.ccq_true_label), true, true);
            addOptionRow(getString(R.string.ccq_false_label), false, true);
            setCorrectIndex(0);
        } else {
            for (int i = 0; i < INITIAL_OPTION_COUNT; i++) {
                addOptionRow(null, i == 0 && selectedQuestionType == QuestionType.SINGLE_CHOICE,
                        false);
            }
        }
        updateOptionControls();
    }

    private void renderOptionEditorFromResponse(List<QuestionOptionResponse> options) {
        binding.containerOptions.removeAllViews();
        optionRows.clear();
        if (options == null || options.isEmpty()) {
            renderDefaultOptionEditor();
            return;
        }
        boolean trueFalse = selectedQuestionType == QuestionType.TRUE_FALSE;
        for (QuestionOptionResponse option : options) {
            addOptionRow(option.getOptionText(), Boolean.TRUE.equals(option.getIsCorrect()),
                    trueFalse);
        }
        if ((selectedQuestionType == QuestionType.SINGLE_CHOICE
                || selectedQuestionType == QuestionType.TRUE_FALSE)
                && selectedCorrectCount() == 0 && !optionRows.isEmpty()) {
            setCorrectIndex(0);
        }
        updateOptionControls();
    }

    private void addOptionRow(String presetText, boolean correct, boolean fixedText) {
        if (selectedQuestionType != QuestionType.TRUE_FALSE && optionRows.size() >= MAX_OPTIONS) {
            return;
        }

        ItemQuestionOptionRowBinding rowBinding =
                ItemQuestionOptionRowBinding.inflate(getLayoutInflater(), binding.containerOptions, false);
        OptionRow row = new OptionRow(rowBinding);
        if (presetText != null) {
            rowBinding.inputOptionText.setText(presetText);
        }
        rowBinding.inputOptionText.setEnabled(!fixedText);
        rowBinding.radioCorrect.setChecked(correct
                && selectedQuestionType != QuestionType.MULTIPLE_CHOICE);
        rowBinding.checkboxCorrect.setChecked(correct
                && selectedQuestionType == QuestionType.MULTIPLE_CHOICE);
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

    private void applyValidationErrors(QuestionFormValidator.ValidationResult result) {
        Map<String, String> errors = result.getFieldErrors();
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
            optionRows.get(i).binding.layoutOptionText.setError(resolveOptionTextError(
                    errors.get(QuestionFormValidator.optionTextKey(i))
            ));
        }

        if (result.getFormError() != null) {
            Toast.makeText(this, resolveFormError(result.getFormError()), Toast.LENGTH_SHORT)
                    .show();
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

    private void setInFlight(boolean loading) {
        inFlight = loading;
        binding.progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        updateActionEnabled();
    }

    private void updateActionEnabled() {
        boolean enabled = !inFlight && !cascadeError;
        binding.btnEdit.setEnabled(!inFlight);
        binding.btnSubmitReview.setEnabled(!inFlight);
        binding.btnSave.setEnabled(enabled);
        binding.btnCancel.setEnabled(!inFlight);
    }

    private void bindEnumBadge(
            TextView view,
            @StringRes int labelRes,
            Enum<?> fallback,
            @ColorRes int colorRes
    ) {
        bindEnumText(view, labelRes, fallback);
        view.setBackgroundColor(ContextCompat.getColor(this, colorRes));
    }

    private void bindEnumText(TextView view, @StringRes int labelRes, Enum<?> fallback) {
        if (labelRes != 0) {
            view.setText(getString(labelRes));
        } else if (fallback != null) {
            view.setText(fallback.name());
        } else {
            view.setText("");
        }
    }

    private void bindIdText(TextView view, Long id, @StringRes int labelRes) {
        view.setVisibility(id != null ? View.VISIBLE : View.GONE);
        if (id != null) {
            view.setText(getString(labelRes, id));
        }
    }

    private void bindOptionalIdText(TextView view, Long id, @StringRes int labelRes) {
        bindIdText(view, id, labelRes);
    }

    private void bindOptionalSection(View section, TextView valueView, String value) {
        boolean visible = !isBlank(value);
        section.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            valueView.setText(value);
        }
    }

    private void bindOptionalFormattedText(TextView view, String value, @StringRes int labelRes) {
        boolean visible = !isBlank(value);
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            view.setText(getString(labelRes, value));
        }
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
                ? getString(R.string.cqd_error_generic)
                : null;
    }

    private String resolveLengthError(String error) {
        return QuestionFormValidator.ERROR_TOO_LONG.equals(error)
                ? getString(R.string.cqd_error_generic)
                : null;
    }

    private String resolveOptionsError(String error) {
        if (QuestionFormValidator.ERROR_OPTIONS_MIN.equals(error)) {
            return getString(R.string.ccq_validation_options_min);
        }
        if (QuestionFormValidator.ERROR_OPTIONS_MAX.equals(error)) {
            return getString(R.string.ccq_validation_options_max);
        }
        return getString(R.string.cqd_error_generic);
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
        return getString(R.string.cqd_error_generic);
    }

    private String messageOrGeneric(String message) {
        return message != null && !message.trim().isEmpty()
                ? message
                : getString(R.string.cqd_error_generic);
    }

    private String reviewActionLabel(ReviewAction action) {
        if (action == ReviewAction.APPROVE) {
            return getString(R.string.cqd_review_action_approve);
        }
        if (action == ReviewAction.REQUEST_REVISION) {
            return getString(R.string.cqd_review_action_request_revision);
        }
        if (action == ReviewAction.REJECT) {
            return getString(R.string.cqd_review_action_reject);
        }
        return action != null ? action.name() : "";
    }

    @ColorRes
    private int reviewActionColor(ReviewAction action) {
        if (action == ReviewAction.APPROVE) {
            return R.color.success;
        }
        if (action == ReviewAction.REQUEST_REVISION) {
            return R.color.warning;
        }
        if (action == ReviewAction.REJECT) {
            return R.color.error;
        }
        return R.color.text_secondary;
    }

    @StringRes
    private int statusBannerRes(QuestionStatus status) {
        if (status == QuestionStatus.PENDING_REVIEW) {
            return R.string.cqd_status_banner_pending;
        }
        if (status == QuestionStatus.APPROVED) {
            return R.string.cqd_status_banner_approved;
        }
        if (status == QuestionStatus.PUBLISHED) {
            return R.string.cqd_status_banner_published;
        }
        if (status == QuestionStatus.HIDDEN) {
            return R.string.cqd_status_banner_hidden;
        }
        if (status == QuestionStatus.ARCHIVED) {
            return R.string.cqd_status_banner_archived;
        }
        return 0;
    }

    private <T> ArrayAdapter<String> labelAdapter(List<DropdownItem<T>> items) {
        List<String> labels = new ArrayList<>();
        for (DropdownItem<T> item : items) {
            labels.add(item.label);
        }
        return new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, labels);
    }

    private <T> String labelFor(List<DropdownItem<T>> items, T value) {
        if (items == null || value == null) {
            return "";
        }
        for (DropdownItem<T> item : items) {
            if (item.value == value || item.value.equals(value)) {
                return item.label;
            }
        }
        return "";
    }

    private String labelForSubject(Long id) {
        for (SubjectResponse subject : subjects) {
            if (id != null && id.equals(subject.getId())) {
                return displayName(subject.getName(), subject.getCode());
            }
        }
        return "";
    }

    private String labelForTopic(Long id) {
        for (TopicResponse topic : topics) {
            if (id != null && id.equals(topic.getId())) {
                return displayName(topic.getName(), topic.getCode());
            }
        }
        return "";
    }

    private String labelForSubtopic(Long id) {
        for (SubtopicResponse subtopic : subtopics) {
            if (id != null && id.equals(subtopic.getId())) {
                return displayName(subtopic.getName(), subtopic.getCode());
            }
        }
        return "";
    }

    private static String textOf(android.widget.TextView view) {
        return view.getText() != null ? view.getText().toString().trim() : "";
    }

    private static String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String nonEmpty(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private static String formatDate(String value) {
        if (isBlank(value)) {
            return "";
        }
        String normalized = value.replace('T', ' ');
        return normalized.length() > 16 ? normalized.substring(0, 16) : normalized;
    }

    private static String displayName(String name, String code) {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        return code != null ? code : "";
    }

    private static String optionLabel(int index, boolean trueFalse) {
        if (trueFalse) {
            return index == 0 ? "\u0110" : "S";
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
