package com.example.v_sat_compass.ui.admin.questions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.enums.Difficulty;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.enums.QuestionType;
import com.example.v_sat_compass.data.model.question.QuestionListItemResponse;

import java.util.ArrayList;
import java.util.List;

public class AdminReviewQueueAdapter
        extends RecyclerView.Adapter<AdminReviewQueueAdapter.ReviewQueueViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(QuestionListItemResponse item);
    }

    private final List<QuestionListItemResponse> items = new ArrayList<>();
    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<QuestionListItemResponse> questions) {
        items.clear();
        if (questions != null) {
            items.addAll(questions);
        }
        notifyDataSetChangedSafely();
    }

    public void clear() {
        items.clear();
        notifyDataSetChangedSafely();
    }

    public QuestionListItemResponse getItemAt(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public ReviewQueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_review_queue_row, parent, false);
        return new ReviewQueueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewQueueViewHolder holder, int position) {
        QuestionListItemResponse question = items.get(position);
        holder.bind(question);
        holder.itemView.setOnClickListener(v -> dispatchClick(question));
    }

    void dispatchClickForTest(int position) {
        QuestionListItemResponse question = getItemAt(position);
        if (question != null) {
            dispatchClick(question);
        }
    }

    private void dispatchClick(QuestionListItemResponse question) {
        if (listener != null) {
            listener.onItemClick(question);
        }
    }

    private void notifyDataSetChangedSafely() {
        try {
            notifyDataSetChanged();
        } catch (Exception ignored) {
            // RecyclerView is not attached in JVM unit tests.
        }
    }

    @StringRes
    static int difficultyLabelRes(Difficulty difficulty) {
        if (difficulty == Difficulty.EASY) {
            return R.string.cq_difficulty_easy;
        }
        if (difficulty == Difficulty.MEDIUM) {
            return R.string.cq_difficulty_medium;
        }
        if (difficulty == Difficulty.HARD) {
            return R.string.cq_difficulty_hard;
        }
        if (difficulty == Difficulty.VERY_HARD) {
            return R.string.cq_difficulty_very_hard;
        }
        return 0;
    }

    @ColorRes
    static int difficultyColorRes(Difficulty difficulty) {
        if (difficulty == Difficulty.EASY) {
            return R.color.cq_difficulty_easy_bg;
        }
        if (difficulty == Difficulty.MEDIUM) {
            return R.color.cq_difficulty_medium_bg;
        }
        if (difficulty == Difficulty.HARD) {
            return R.color.cq_difficulty_hard_bg;
        }
        if (difficulty == Difficulty.VERY_HARD) {
            return R.color.cq_difficulty_very_hard_bg;
        }
        return R.color.text_secondary;
    }

    @StringRes
    static int questionTypeLabelRes(QuestionType type) {
        if (type == QuestionType.SINGLE_CHOICE) {
            return R.string.cq_type_single_choice;
        }
        if (type == QuestionType.MULTIPLE_CHOICE) {
            return R.string.cq_type_multiple_choice;
        }
        if (type == QuestionType.TRUE_FALSE) {
            return R.string.cq_type_true_false;
        }
        if (type == QuestionType.FILL_IN_BLANK) {
            return R.string.cq_type_fill_in_blank;
        }
        return 0;
    }

    @StringRes
    static int statusLabelRes(QuestionStatus status) {
        if (status == QuestionStatus.DRAFT) {
            return R.string.cq_status_draft;
        }
        if (status == QuestionStatus.PENDING_REVIEW) {
            return R.string.cq_status_pending_review;
        }
        if (status == QuestionStatus.NEEDS_REVISION) {
            return R.string.cq_status_needs_revision;
        }
        if (status == QuestionStatus.APPROVED) {
            return R.string.cq_status_approved;
        }
        if (status == QuestionStatus.PUBLISHED) {
            return R.string.cq_status_published;
        }
        if (status == QuestionStatus.HIDDEN) {
            return R.string.cq_status_hidden;
        }
        if (status == QuestionStatus.ARCHIVED) {
            return R.string.cq_status_archived;
        }
        return 0;
    }

    @ColorRes
    static int statusColorRes(QuestionStatus status) {
        if (status == QuestionStatus.DRAFT) {
            return R.color.cq_status_draft_bg;
        }
        if (status == QuestionStatus.PENDING_REVIEW) {
            return R.color.cq_status_pending_review_bg;
        }
        if (status == QuestionStatus.NEEDS_REVISION) {
            return R.color.cq_status_needs_revision_bg;
        }
        if (status == QuestionStatus.APPROVED) {
            return R.color.cq_status_approved_bg;
        }
        if (status == QuestionStatus.PUBLISHED) {
            return R.color.cq_status_published_bg;
        }
        if (status == QuestionStatus.HIDDEN) {
            return R.color.cq_status_hidden_bg;
        }
        if (status == QuestionStatus.ARCHIVED) {
            return R.color.cq_status_archived_bg;
        }
        return R.color.text_secondary;
    }

    static String displayQuestionText(QuestionListItemResponse question) {
        if (question == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (question.getSubjectId() != null) {
            parts.add("Subject #" + question.getSubjectId());
        }
        if (question.getTopicId() != null) {
            parts.add("Topic #" + question.getTopicId());
        }
        if (!parts.isEmpty()) {
            return join(parts);
        }
        return "Question #" + (question.getId() != null ? question.getId() : "");
    }

    static String formatUpdatedAt(String updatedAt) {
        if (updatedAt == null || updatedAt.trim().isEmpty()) {
            return "";
        }
        String normalized = updatedAt.replace('T', ' ');
        return normalized.length() > 16 ? normalized.substring(0, 16) : normalized;
    }

    private static String join(List<String> parts) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                builder.append(" / ");
            }
            builder.append(parts.get(i));
        }
        return builder.toString();
    }

    static class ReviewQueueViewHolder extends RecyclerView.ViewHolder {
        private final TextView textQuestionCode;
        private final TextView textStatusBadge;
        private final TextView textDifficultyBadge;
        private final TextView textQuestionTypeBadge;
        private final TextView textQuestionText;
        private final TextView textUpdatedAt;

        ReviewQueueViewHolder(@NonNull View itemView) {
            super(itemView);
            textQuestionCode = itemView.findViewById(R.id.textQuestionCode);
            textStatusBadge = itemView.findViewById(R.id.textStatusBadge);
            textDifficultyBadge = itemView.findViewById(R.id.textDifficultyBadge);
            textQuestionTypeBadge = itemView.findViewById(R.id.textQuestionTypeBadge);
            textQuestionText = itemView.findViewById(R.id.textQuestionText);
            textUpdatedAt = itemView.findViewById(R.id.textUpdatedAt);
        }

        void bind(QuestionListItemResponse question) {
            textQuestionCode.setText(question.getQuestionCode() != null
                    ? question.getQuestionCode()
                    : "");
            bindLabel(textStatusBadge, statusLabelRes(question.getStatus()), question.getStatus());
            bindLabel(textDifficultyBadge, difficultyLabelRes(question.getDifficulty()),
                    question.getDifficulty());
            bindLabel(textQuestionTypeBadge, questionTypeLabelRes(question.getQuestionType()),
                    question.getQuestionType());
            textQuestionText.setText(displayQuestionText(question));
            textUpdatedAt.setText(formatUpdatedAt(question.getUpdatedAt()));

            textStatusBadge.setBackgroundColor(ContextCompat.getColor(
                    textStatusBadge.getContext(),
                    statusColorRes(question.getStatus())
            ));
            textDifficultyBadge.setBackgroundColor(ContextCompat.getColor(
                    textDifficultyBadge.getContext(),
                    difficultyColorRes(question.getDifficulty())
            ));
        }

        private void bindLabel(TextView view, @StringRes int resId, Enum<?> fallback) {
            if (resId != 0) {
                view.setText(view.getContext().getString(resId));
            } else if (fallback != null) {
                view.setText(fallback.name());
            } else {
                view.setText("");
            }
        }
    }
}
