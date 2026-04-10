package com.example.v_sat_compass.ui.admin.questions;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.v_sat_compass.data.model.QuestionItem;
import com.example.v_sat_compass.databinding.ItemQuestionBankBinding;

import java.util.ArrayList;
import java.util.List;

public class QuestionBankAdapter extends RecyclerView.Adapter<QuestionBankAdapter.ViewHolder> {

    private List<QuestionItem> items = new ArrayList<>();
    private OnQuestionClickListener listener;

    public interface OnQuestionClickListener {
        void onClick(QuestionItem question);
    }

    public void setOnQuestionClickListener(OnQuestionClickListener l) {
        this.listener = l;
    }

    public void setQuestions(List<QuestionItem> list) {
        this.items = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemQuestionBankBinding binding = ItemQuestionBankBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuestionItem q = items.get(position);
        holder.binding.tvQuestionCode.setText(q.getQuestionCode() != null ? q.getQuestionCode() : "—");
        holder.binding.tvSubject.setText(q.getSubjectName() != null ? q.getSubjectName() : "—");
        holder.binding.tvType.setText(q.getQuestionTypeLabel() != null ? q.getQuestionTypeLabel() : "Trắc nghiệm");
        holder.binding.tvStatus.setText(q.getStatusLabel());
        holder.binding.tvViewCount.setText(String.valueOf(q.getViewCount()));
        holder.binding.tvFlagCount.setText("🚩 " + q.getFlagCount());

        // Đặt màu badge trạng thái
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(40f);
        bg.setColor(q.getStatusColor());
        holder.binding.tvStatus.setBackground(bg);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(q);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemQuestionBankBinding binding;
        ViewHolder(ItemQuestionBankBinding b) {
            super(b.getRoot());
            this.binding = b;
        }
    }

    /** Thêm tiện ích cho QuestionItem */
    private String getQuestionTypeLabel(QuestionItem q) {
        if (q.getQuestionType() == null) return "Trắc nghiệm";
        return "SHORT_ANSWER".equals(q.getQuestionType()) ? "Tự luận" : "Trắc nghiệm";
    }
}
