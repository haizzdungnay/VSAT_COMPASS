package com.example.v_sat_compass.ui.home;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.Exam;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> {

    private final List<Exam> exams;
    private final OnExamClickListener listener;

    public interface OnExamClickListener {
        void onExamClick(Exam exam);
    }

    public SuggestionAdapter(List<Exam> exams, OnExamClickListener listener) {
        this.exams = exams;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exam exam = exams.get(position);
        holder.tvTitle.setText(exam.getTitle());
        holder.tvSubject.setText(exam.getSubjectName() != null ? exam.getSubjectName() : "");
        
        Context context = holder.itemView.getContext();
        holder.tvDuration.setText(context.getString(R.string.suggestion_duration_format, exam.getDurationMinutes()));
        holder.tvQuestions.setText(context.getString(R.string.suggestion_questions_format, exam.getTotalQuestions()));

        // Format info fallback to keep legacy code compatibility
        holder.tvInfo.setText(exam.getDurationMinutes() + " ph • " + exam.getTotalQuestions() + " câu");

        String subject = exam.getSubjectName() != null ? exam.getSubjectName() : "";
        if (subject.toLowerCase().contains("toán")) {
            setIconBgColor(holder.layoutIcon, context.getResources().getColor(R.color.math_icon_bg));
            holder.ivIcon.setColorFilter(context.getResources().getColor(R.color.math_icon_tint));
            holder.ivAction.setImageResource(R.drawable.ic_bookmark);
            holder.ivAction.setColorFilter(context.getResources().getColor(R.color.math_icon_tint));
        } else if (subject.toLowerCase().contains("anh") || subject.toLowerCase().contains("english")) {
            setIconBgColor(holder.layoutIcon, context.getResources().getColor(R.color.english_icon_bg));
            holder.ivIcon.setColorFilter(context.getResources().getColor(R.color.english_icon_tint));
            holder.ivAction.setImageResource(R.drawable.ic_flag);
            holder.ivAction.setColorFilter(context.getResources().getColor(R.color.english_icon_tint));
        } else {
            setIconBgColor(holder.layoutIcon, context.getResources().getColor(R.color.background));
            holder.ivIcon.setColorFilter(context.getResources().getColor(R.color.primary));
            holder.ivAction.setImageResource(R.drawable.ic_bookmark);
            holder.ivAction.setColorFilter(context.getResources().getColor(R.color.primary));
        }

        holder.card.setOnClickListener(v -> listener.onExamClick(exam));
    }

    private void setIconBgColor(View view, int color) {
        Drawable background = view.getBackground();
        if (background instanceof GradientDrawable) {
            ((GradientDrawable) background).setColor(color);
        } else {
            view.setBackgroundColor(color);
        }
    }

    @Override
    public int getItemCount() {
        return exams.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvTitle, tvSubject, tvInfo, tvDuration, tvQuestions;
        View layoutIcon;
        ImageView ivIcon, ivAction;

        ViewHolder(View view) {
            super(view);
            card = view.findViewById(R.id.cardSuggestion);
            tvTitle = view.findViewById(R.id.tvSuggestionTitle);
            tvSubject = view.findViewById(R.id.tvSuggestionSubject);
            tvInfo = view.findViewById(R.id.tvSuggestionInfo);
            tvDuration = view.findViewById(R.id.tvSuggestionDuration);
            tvQuestions = view.findViewById(R.id.tvSuggestionQuestions);
            layoutIcon = view.findViewById(R.id.layoutSuggestionIcon);
            ivIcon = view.findViewById(R.id.ivSuggestionIcon);
            ivAction = view.findViewById(R.id.ivSuggestionAction);
        }
    }
}
