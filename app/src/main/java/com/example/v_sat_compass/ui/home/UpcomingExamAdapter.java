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

public class UpcomingExamAdapter extends RecyclerView.Adapter<UpcomingExamAdapter.ViewHolder> {

    private final List<Exam> exams;
    private final OnExamClickListener listener;

    public interface OnExamClickListener {
        void onExamClick(Exam exam);
    }

    public UpcomingExamAdapter(List<Exam> exams, OnExamClickListener listener) {
        this.exams = exams;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_upcoming_exam, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exam exam = exams.get(position);
        holder.tvTitle.setText(exam.getTitle() != null ? exam.getTitle() : "");
        
        long baseTimestamp = 1715734800000L; // 15/05/2024, 08:00
        long examId = exam.getId() != null ? exam.getId() : 1;
        long timeOffset = (examId - 1) * 24L * 60 * 60 * 1000; // 1 day offset per exam to differentiate them
        java.util.Date scheduledDate = new java.util.Date(baseTimestamp + timeOffset);
        String pattern = holder.itemView.getContext().getString(R.string.upcoming_exam_date_pattern);
        String formattedDate = android.text.format.DateFormat.format(pattern, scheduledDate).toString();
        holder.tvInfo.setText(formattedDate);

        holder.tvSubject.setText(exam.getSubjectName() != null ? exam.getSubjectName() : "");
        holder.card.setOnClickListener(v -> {
            if (listener != null) listener.onExamClick(exam);
        });
    }

    @Override
    public int getItemCount() {
        return exams.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvTitle, tvInfo, tvSubject;

        ViewHolder(View view) {
            super(view);
            card = (MaterialCardView) view;
            tvTitle = view.findViewById(R.id.tvUpcomingTitle);
            tvInfo = view.findViewById(R.id.tvUpcomingInfo);
            tvSubject = view.findViewById(R.id.tvUpcomingSubject);
        }
    }
}
