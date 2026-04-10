package com.example.v_sat_compass.ui.exam.session;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.v_sat_compass.R;

import java.util.Map;
import java.util.Set;

public class QuestionGridAdapter extends RecyclerView.Adapter<QuestionGridAdapter.ViewHolder> {

    public interface OnQuestionClickListener {
        void onQuestionClick(int position);
    }

    private final int totalQuestions;
    private final Map<Long, Long> answeredQuestions;
    private final Set<Long> bookmarkedQuestions;
    private final java.util.List<Long> questionIds;
    private final int currentIndex;
    private final OnQuestionClickListener listener;

    public QuestionGridAdapter(int totalQuestions, java.util.List<Long> questionIds,
                               Map<Long, Long> answeredQuestions, Set<Long> bookmarkedQuestions,
                               int currentIndex, OnQuestionClickListener listener) {
        this.totalQuestions = totalQuestions;
        this.questionIds = questionIds;
        this.answeredQuestions = answeredQuestions;
        this.bookmarkedQuestions = bookmarkedQuestions;
        this.currentIndex = currentIndex;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_question_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvQuestionNum.setText(String.valueOf(position + 1));

        Long questionId = position < questionIds.size() ? questionIds.get(position) : null;
        boolean isAnswered = questionId != null && answeredQuestions.containsKey(questionId);
        boolean isBookmarked = questionId != null && bookmarkedQuestions.contains(questionId);
        boolean isCurrent = position == currentIndex;

        if (isCurrent) {
            holder.tvQuestionNum.setBackgroundResource(R.drawable.bg_question_current);
            holder.tvQuestionNum.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.primary));
        } else if (isAnswered) {
            holder.tvQuestionNum.setBackgroundResource(R.drawable.bg_question_answered);
            holder.tvQuestionNum.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.white));
        } else {
            holder.tvQuestionNum.setBackgroundResource(R.drawable.bg_question_unanswered);
            holder.tvQuestionNum.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_primary));
        }

        holder.ivBookmark.setVisibility(isBookmarked ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onQuestionClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return totalQuestions;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestionNum;
        ImageView ivBookmark;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestionNum = itemView.findViewById(R.id.tvQuestionNum);
            ivBookmark = itemView.findViewById(R.id.ivBookmark);
        }
    }
}