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
import com.example.v_sat_compass.data.local.LocalExamDataSource;
import com.example.v_sat_compass.data.model.Question;

import java.util.List;
import java.util.Map;

/**
 * Grid adapter cho màn xem lời giải — hiển thị trạng thái đúng/sai/bỏ qua từng câu.
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

        // Tính đúng/sai qua LocalExamDataSource
        boolean isCorrect = false;
        if (isAnswered) {
            Question q = LocalExamDataSource.getInstance().getQuestion(
                    holder.itemView.getContext(), questionId);
            if (q != null && q.getOptions() != null) {
                for (Question.Option opt : q.getOptions()) {
                    if (opt.getId() != null && opt.getId().equals(selectedOptionId)) {
                        isCorrect = opt.isCorrect();
                        break;
                    }
                }
            }
        }

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
        } else if (isCorrect) {
            // Đúng → xanh lá
            holder.itemView.setBackgroundColor(Color.parseColor("#E8F5E9"));
            holder.tvNumber.setTextColor(Color.parseColor("#388E3C"));
        } else {
            // Sai → đỏ nhạt
            holder.itemView.setBackgroundColor(Color.parseColor("#FFEBEE"));
            holder.tvNumber.setTextColor(Color.parseColor("#D32F2F"));
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
