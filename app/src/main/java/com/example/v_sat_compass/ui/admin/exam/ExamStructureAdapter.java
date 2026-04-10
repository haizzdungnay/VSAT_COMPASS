package com.example.v_sat_compass.ui.admin.exam;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.v_sat_compass.data.model.ExamStructureQuestion;
import com.example.v_sat_compass.databinding.ItemExamQuestionRowBinding;

import java.util.ArrayList;
import java.util.List;

public class ExamStructureAdapter extends RecyclerView.Adapter<ExamStructureAdapter.VH> {

    private final List<ExamStructureQuestion> items = new ArrayList<>();
    private OnRemoveListener removeListener;

    public interface OnRemoveListener {
        void onRemove(int position);
    }

    public void setOnRemoveListener(OnRemoveListener l) { this.removeListener = l; }

    public void addQuestion(ExamStructureQuestion q) {
        items.add(q);
        notifyItemInserted(items.size() - 1);
    }

    public void removeAt(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
            // Cập nhật lại số thứ tự
            notifyItemRangeChanged(position, items.size());
        }
    }

    public List<ExamStructureQuestion> getItems() { return new ArrayList<>(items); }

    public int getTotalQuestions() { return items.size(); }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemExamQuestionRowBinding b = ItemExamQuestionRowBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ExamStructureQuestion q = items.get(position);
        holder.b.tvOrder.setText(String.valueOf(position + 1));
        holder.b.tvQuestionCode.setText(q.getPreviewContent() != null
                ? q.getPreviewContent() : "Câu " + (position + 1));
        holder.b.tvType.setText(q.getQuestionTypeLabel());
        holder.b.btnRemove.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (removeListener != null && pos != RecyclerView.NO_ID) removeListener.onRemove(pos);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ItemExamQuestionRowBinding b;
        VH(ItemExamQuestionRowBinding b) { super(b.getRoot()); this.b = b; }
    }
}
