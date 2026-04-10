package com.example.v_sat_compass.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        holder.tvInfo.setText(exam.getDurationMinutes() + " phút • " + exam.getTotalQuestions() + " câu");
        holder.card.setOnClickListener(v -> listener.onExamClick(exam));
    }

    @Override
    public int getItemCount() {
        return exams.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvTitle, tvSubject, tvInfo;

        ViewHolder(View view) {
            super(view);
            card = view.findViewById(R.id.cardSuggestion);
            tvTitle = view.findViewById(R.id.tvSuggestionTitle);
            tvSubject = view.findViewById(R.id.tvSuggestionSubject);
            tvInfo = view.findViewById(R.id.tvSuggestionInfo);
        }
    }
}
