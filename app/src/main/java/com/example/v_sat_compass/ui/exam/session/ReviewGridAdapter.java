package com.example.v_sat_compass.ui.exam.session;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.v_sat_compass.R;

import java.util.List;
import java.util.Map;

/**
 * Grid adapter cho màn xem lời giải — hiển thị trạng thái đã chọn/bỏ qua từng câu.
 */
public class ReviewGridAdapter extends RecyclerView.Adapter<ReviewGridAdapter.ViewHolder> {

    public interface OnQuestionClickListener {
        void onQuestionClick(int position);
    }

    private final List<Long> questionIds;
    // questionId -> selectedOptionId
    private final Map<Long, Long> selectedAnswers;
    private final int currentIndex;
    private final OnQuestionClickListener listener;

    public ReviewGridAdapter(List<Long> questionIds, Map<Long, Long> selectedAnswers,
                             int currentIndex, OnQuestionClickListener listener) {
        this.questionIds = questionIds;
        this.selectedAnswers = selectedAnswers;
        this.currentIndex = currentIndex;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_question_grid, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Long questionId = questionIds.get(position);
        Long selectedOptionId = selectedAnswers.get(questionId);
        boolean isAnswered = selectedOptionId != null;

        holder.tvNumber.setText(String.valueOf(position + 1));

        if (position == currentIndex) {
            // Câu đang xem → viền xanh
            holder.itemView.setBackgroundResource(R.drawable.bg_question_current);
            holder.tvNumber.setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(), R.color.primary));
        } else if (!isAnswered) {
            // Bỏ qua → xám
            holder.itemView.setBackgroundResource(R.drawable.bg_question_unanswered);
            holder.tvNumber.setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(), R.color.text_secondary));
        } else {
            // Đã trả lời → xanh dương nhạt (chấm điểm do server phụ trách)
            holder.itemView.setBackgroundColor(Color.parseColor("#E3F2FD"));
            holder.tvNumber.setTextColor(Color.parseColor("#1565C0"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onQuestionClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return questionIds.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumber;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.tvQuestionNumber);
        }
    }
}
