package com.example.v_sat_compass.ui.admin.questions;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.enums.ReviewAction;
import com.example.v_sat_compass.data.model.question.QuestionOptionResponse;
import com.example.v_sat_compass.data.model.question.QuestionResponse;
import com.example.v_sat_compass.data.model.question.QuestionReviewResponse;
import com.example.v_sat_compass.data.repository.Resource;
import com.example.v_sat_compass.databinding.ActivityAdminReviewQuestionBinding;
import com.example.v_sat_compass.databinding.ItemQuestionOptionViewBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

public class AdminReviewQuestionActivity extends AppCompatActivity {

    public static final String EXTRA_QUESTION_ID = "question_id";

    private ActivityAdminReviewQuestionBinding binding;
    private AdminReviewViewModel viewModel;
    private Long questionId;
    private boolean inFlight;
    private boolean reviewHistoryExpanded = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminReviewQuestionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AdminReviewViewModel.class);
        questionId = getIntent().getLongExtra(EXTRA_QUESTION_ID, -1L);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnRetry.setOnClickListener(v -> {
            if (isValidQuestionId()) {
                viewModel.loadDetail(questionId);
            }
        });
        binding.btnApprove.setOnClickListener(v -> showActionDialog(
                "Duyệt câu hỏi",
                "Bình luận tùy chọn",
                "Duyệt",
                false,
                comment -> viewModel.approve(questionId, comment)
        ));
        binding.btnRequestRevision.setOnClickListener(v -> showActionDialog(
                "Yêu cầu sửa",
                "Nhập yêu cầu chỉnh sửa",
                "Gửi yêu cầu",
                true,
                comment -> viewModel.requestRevision(questionId, comment)
        ));
        binding.btnReject.setOnClickListener(v -> showActionDialog(
                "Từ chối câu hỏi",
                "Nhập lý do từ chối",
                "Từ chối",
                true,
                comment -> viewModel.reject(questionId, comment)
        ));
        binding.cardReviewHistory.setOnClickListener(v -> toggleReviewHistory());

        observeViewModel();

        if (!isValidQuestionId()) {
            showError(getString(R.string.cqd_error_missing_id), false);
            return;
        }
        viewModel.loadDetail(questionId);
    }

    private void observeViewModel() {
        viewModel.getDetailState().observe(this, resource -> {
            if (resource == null) {
                return;
            }
            if (resource.getStatus() == Resource.Status.LOADING) {
                showLoading();
            } else if (resource.getStatus() == Resource.Status.SUCCESS) {
                setInFlight(false);
                renderQuestion(resource.getData());
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                setInFlight(false);
                showError(resource.getMessage(), true);
            }
        });

        viewModel.getActionState().observe(this, resource -> {
            if (resource == null) {
                return;
            }
            if (resource.getStatus() == Resource.Status.LOADING) {
                setInFlight(true);
            } else if (resource.getStatus() == Resource.Status.SUCCESS) {
                setInFlight(false);
                Toast.makeText(this, "Đã cập nhật trạng thái câu hỏi", Toast.LENGTH_SHORT).show();
                setResult(Activity.RESULT_OK);
                finish();
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                setInFlight(false);
                Snackbar.make(binding.getRoot(), messageOrGeneric(resource.getMessage()),
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading() {
        binding.progressLoading.setVisibility(View.VISIBLE);
        binding.errorContainer.setVisibility(View.GONE);
        binding.contentScroll.setVisibility(View.GONE);
        binding.actionContainer.setVisibility(View.GONE);
        binding.textReadOnlyBanner.setVisibility(View.GONE);
    }

    private void showError(String message, boolean canRetry) {
        binding.progressLoading.setVisibility(View.GONE);
        binding.contentScroll.setVisibility(View.GONE);
        binding.actionContainer.setVisibility(View.GONE);
        binding.textReadOnlyBanner.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.VISIBLE);
        binding.btnRetry.setVisibility(canRetry ? View.VISIBLE : View.GONE);
        binding.textErrorMessage.setText(messageOrGeneric(message));
    }

    private void renderQuestion(QuestionResponse question) {
        if (question == null) {
            showError(getString(R.string.cqd_error_load), true);
            return;
        }

        binding.progressLoading.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.GONE);
        binding.contentScroll.setVisibility(View.VISIBLE);

        binding.textQuestionCode.setText(nonEmpty(question.getQuestionCode(), ""));
        bindEnumBadge(binding.textStatusBadge,
                AdminReviewQueueAdapter.statusLabelRes(question.getStatus()),
                question.getStatus(),
                AdminReviewQueueAdapter.statusColorRes(question.getStatus()));
        bindEnumBadge(binding.textDifficultyBadge,
                AdminReviewQueueAdapter.difficultyLabelRes(question.getDifficulty()),
                question.getDifficulty(),
                AdminReviewQueueAdapter.difficultyColorRes(question.getDifficulty()));
        bindEnumText(binding.textQuestionTypeBadge,
                AdminReviewQueueAdapter.questionTypeLabelRes(question.getQuestionType()),
                question.getQuestionType());

        bindIdText(binding.textSubject, question.getSubjectId(), R.string.cqd_label_subject_id);
        bindIdText(binding.textTopic, question.getTopicId(), R.string.cqd_label_topic_id);
        bindIdText(binding.textSubtopic, question.getSubtopicId(), R.string.cqd_label_subtopic_id);
        binding.textQuestionText.setText(nonEmpty(question.getQuestionText(), ""));
        renderOptionViews(question.getOptions());
        bindOptionalSection(binding.sectionExplanation, binding.textExplanation,
                question.getExplanation());
        bindOptionalFormattedText(binding.textSource, question.getSource(),
                R.string.cqd_label_source);
        bindOptionalFormattedText(binding.textTags, question.getTags(), R.string.cqd_label_tags);
        bindOptionalFormattedText(binding.textImageUrl, question.getImageUrl(),
                R.string.cqd_label_image_url);
        bindActorText(binding.textCreatedBy, "Người tạo", question.getCreatedBy());
        bindActorText(binding.textReviewedBy, "Người duyệt", question.getReviewedBy());
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
        boolean reviewable = status == QuestionStatus.PENDING_REVIEW;
        binding.actionContainer.setVisibility(reviewable ? View.VISIBLE : View.GONE);
        binding.textReadOnlyBanner.setVisibility(reviewable ? View.GONE : View.VISIBLE);
        if (!reviewable) {
            binding.textReadOnlyBanner.setText(statusBannerText(status));
        }
        updateActionEnabled();
    }

    private void renderOptionViews(List<QuestionOptionResponse> options) {
        binding.containerOptionViews.removeAllViews();
        if (options == null || options.isEmpty()) {
            TextView empty = secondaryText("Không có đáp án");
            binding.containerOptionViews.addView(empty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < options.size(); i++) {
            QuestionOptionResponse option = options.get(i);
            ItemQuestionOptionViewBinding rowBinding = ItemQuestionOptionViewBinding.inflate(
                    inflater,
                    binding.containerOptionViews,
                    false
            );
            rowBinding.textOptionLabel.setText(nonEmpty(option.getOptionLabel(),
                    optionLabel(i)));
            rowBinding.textOptionText.setText(nonEmpty(option.getOptionText(), ""));
            rowBinding.badgeCorrect.setVisibility(Boolean.TRUE.equals(option.getIsCorrect())
                    ? View.VISIBLE
                    : View.GONE);
            binding.containerOptionViews.addView(rowBinding.getRoot());
        }
    }

    private void renderReviewHistory(List<QuestionReviewResponse> reviews) {
        binding.containerReviewHistory.removeAllViews();
        binding.cardReviewHistory.setVisibility(View.VISIBLE);
        binding.reviewHistoryBody.setVisibility(reviewHistoryExpanded ? View.VISIBLE : View.GONE);
        if (reviews == null || reviews.isEmpty()) {
            binding.containerReviewHistory.addView(secondaryText("Chưa có lịch sử duyệt"));
            return;
        }

        for (QuestionReviewResponse review : reviews) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));

            TextView action = primaryText(reviewActionLabel(review.getAction()));
            action.setTextColor(ContextCompat.getColor(this, reviewActionColor(review.getAction())));
            row.addView(action);

            if (review.getReviewerId() != null) {
                row.addView(secondaryText(getString(R.string.cqd_label_reviewer_id,
                        review.getReviewerId())));
            }
            if (review.getVersionReviewed() != null) {
                row.addView(secondaryText(getString(R.string.cqd_label_version_reviewed,
                        review.getVersionReviewed())));
            }
            if (!isBlank(review.getCreatedAt())) {
                row.addView(secondaryText(formatDate(review.getCreatedAt())));
            }
            if (!isBlank(review.getComment())) {
                TextView comment = primaryText(review.getComment());
                comment.setPadding(0, dp(4), 0, 0);
                row.addView(comment);
            }

            binding.containerReviewHistory.addView(row);
        }
    }

    private void toggleReviewHistory() {
        reviewHistoryExpanded = !reviewHistoryExpanded;
        binding.reviewHistoryBody.setVisibility(reviewHistoryExpanded ? View.VISIBLE : View.GONE);
    }

    private void showActionDialog(
            String title,
            String hint,
            String positiveText,
            boolean required,
            CommentSubmitter submitter
    ) {
        TextInputLayout inputLayout = new TextInputLayout(this);
        inputLayout.setHint(hint);
        inputLayout.setHelperText("0/" + AdminReviewViewModel.MAX_COMMENT_LENGTH);

        TextInputEditText input = new TextInputEditText(inputLayout.getContext());
        input.setMinLines(3);
        input.setMaxLines(6);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        inputLayout.addView(input);

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                inputLayout.setHelperText(s.length() + "/"
                        + AdminReviewViewModel.MAX_COMMENT_LENGTH);
                inputLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(inputLayout)
                .setPositiveButton(positiveText, null)
                .setNegativeButton(R.string.cqd_confirm_cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String comment = input.getText() != null
                            ? input.getText().toString()
                            : "";
                    if (!validateComment(inputLayout, comment, required)) {
                        return;
                    }
                    dialog.dismiss();
                    submitter.submit(comment.trim());
                }));
        dialog.show();
    }

    private boolean validateComment(TextInputLayout layout, String comment, boolean required) {
        if (required && isBlank(comment)) {
            layout.setError("Vui lòng nhập bình luận");
            return false;
        }
        if (comment != null && comment.length() > AdminReviewViewModel.MAX_COMMENT_LENGTH) {
            layout.setError("Bình luận tối đa 2000 ký tự");
            return false;
        }
        return true;
    }

    private void setInFlight(boolean loading) {
        inFlight = loading;
        binding.progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        updateActionEnabled();
    }

    private void updateActionEnabled() {
        boolean enabled = !inFlight;
        binding.btnApprove.setEnabled(enabled);
        binding.btnRequestRevision.setEnabled(enabled);
        binding.btnReject.setEnabled(enabled);
        binding.btnRetry.setEnabled(enabled);
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

    private void bindActorText(TextView view, String label, Long id) {
        view.setVisibility(id != null ? View.VISIBLE : View.GONE);
        if (id != null) {
            view.setText(label + " #" + id);
        }
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

    private String statusBannerText(QuestionStatus status) {
        if (status == QuestionStatus.APPROVED) {
            return getString(R.string.cqd_status_banner_approved);
        }
        if (status == QuestionStatus.PUBLISHED) {
            return getString(R.string.cqd_status_banner_published);
        }
        if (status == QuestionStatus.HIDDEN) {
            return getString(R.string.cqd_status_banner_hidden);
        }
        if (status == QuestionStatus.ARCHIVED) {
            return getString(R.string.cqd_status_banner_archived);
        }
        if (status == QuestionStatus.NEEDS_REVISION) {
            return getString(R.string.cq_status_needs_revision);
        }
        if (status == QuestionStatus.DRAFT) {
            return getString(R.string.cq_status_draft);
        }
        return "";
    }

    private TextView primaryText(String value) {
        TextView view = new TextView(this);
        view.setText(nonEmpty(value, ""));
        view.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        view.setTextSize(14);
        return view;
    }

    private TextView secondaryText(String value) {
        TextView view = new TextView(this);
        view.setText(nonEmpty(value, ""));
        view.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        view.setTextSize(13);
        return view;
    }

    private boolean isValidQuestionId() {
        return questionId != null && questionId > 0L;
    }

    private String messageOrGeneric(String message) {
        return !isBlank(message) ? message : getString(R.string.cqd_error_generic);
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

    private static String optionLabel(int index) {
        return String.valueOf((char) ('A' + index));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface CommentSubmitter {
        void submit(String comment);
    }
}
