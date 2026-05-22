package com.example.v_sat_compass.ui.admin.exam;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.admin.QuestionPickerItemResponse;
import com.example.v_sat_compass.data.model.enums.Difficulty;
import com.example.v_sat_compass.data.model.enums.QuestionType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AdminQuestionPickerAdapter
        extends RecyclerView.Adapter<AdminQuestionPickerAdapter.PickerViewHolder> {

    public interface OnSelectionToggleListener {
        void onToggle(Long id);
    }

    private final List<QuestionPickerItemResponse> items = new ArrayList<>();
    private Set<Long> selectedIds = new LinkedHashSet<>();
    private OnSelectionToggleListener selectionToggleListener;

    public void setOnSelectionToggleListener(OnSelectionToggleListener listener) {
        this.selectionToggleListener = listener;
    }

    public void setItems(List<QuestionPickerItemResponse> questions) {
        items.clear();
        if (questions != null) {
            items.addAll(questions);
        }
        notifyDataSetChangedSafely();
    }

    public void updateSelectionState(Set<Long> selectedIds) {
        this.selectedIds = selectedIds == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(selectedIds);
        notifyDataSetChangedSafely();
    }

    public QuestionPickerItemResponse getItemAt(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    public boolean isSelectedForTest(Long id) {
        return selectedIds.contains(id);
    }

    void dispatchClickForTest(int position) {
        QuestionPickerItemResponse item = getItemAt(position);
        if (item != null) {
            dispatchToggle(item.getId());
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public PickerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_question_picker_row, parent, false);
        return new PickerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PickerViewHolder holder, int position) {
        QuestionPickerItemResponse question = items.get(position);
        holder.bind(question, selectedIds.contains(question.getId()));
        holder.itemView.setOnClickListener(v -> dispatchToggle(question.getId()));
        holder.checkboxSelect.setOnClickListener(v -> dispatchToggle(question.getId()));
    }

    private void dispatchToggle(Long id) {
        if (selectionToggleListener != null) {
            selectionToggleListener.onToggle(id);
        }
    }

    private void notifyDataSetChangedSafely() {
        try {
            notifyDataSetChanged();
        } catch (Exception ignored) {
            // RecyclerView is not attached in JVM unit tests.
        }
    }

    static String displayQuestionCode(QuestionPickerItemResponse question) {
        if (question == null || question.getQuestionCode() == null) {
            return "";
        }
        return question.getQuestionCode();
    }

    static String displaySnippet(QuestionPickerItemResponse question) {
        if (question == null || question.getQuestionTextSnippet() == null) {
            return "";
        }
        String snippet = question.getQuestionTextSnippet();
        return snippet.length() <= 200 ? snippet : snippet.substring(0, 200) + "…";
    }

    static String difficultyLabel(Difficulty difficulty) {
        if (difficulty == Difficulty.EASY) {
            return "Dễ";
        }
        if (difficulty == Difficulty.MEDIUM) {
            return "Trung bình";
        }
        if (difficulty == Difficulty.HARD) {
            return "Khó";
        }
        if (difficulty == Difficulty.VERY_HARD) {
            return "Rất khó";
        }
        return "";
    }

    static String questionTypeLabel(QuestionType type) {
        if (type == QuestionType.SINGLE_CHOICE) {
            return "1 đáp án";
        }
        if (type == QuestionType.MULTIPLE_CHOICE) {
            return "Nhiều đáp án";
        }
        if (type == QuestionType.TRUE_FALSE) {
            return "Đúng / Sai";
        }
        if (type == QuestionType.FILL_IN_BLANK) {
            return "Điền khuyết";
        }
        return "";
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

    static class PickerViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox checkboxSelect;
        private final TextView textQuestionCode;
        private final TextView textSnippet;
        private final TextView textDifficulty;
        private final TextView textQuestionType;

        PickerViewHolder(@NonNull View itemView) {
            super(itemView);
            checkboxSelect = itemView.findViewById(R.id.checkboxSelect);
            textQuestionCode = itemView.findViewById(R.id.textQuestionCode);
            textSnippet = itemView.findViewById(R.id.textSnippet);
            textDifficulty = itemView.findViewById(R.id.textDifficulty);
            textQuestionType = itemView.findViewById(R.id.textQuestionType);
        }

        void bind(QuestionPickerItemResponse question, boolean selected) {
            checkboxSelect.setChecked(selected);
            textQuestionCode.setText(displayQuestionCode(question));
            textSnippet.setText(displaySnippet(question));
            textDifficulty.setText(difficultyLabel(question.getDifficulty()));
            textDifficulty.setBackgroundResource(difficultyColorRes(question.getDifficulty()));
            textQuestionType.setText(questionTypeLabel(question.getQuestionType()));
        }
    }
}
