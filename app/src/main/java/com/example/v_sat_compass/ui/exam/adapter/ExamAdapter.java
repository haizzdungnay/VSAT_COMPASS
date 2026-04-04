package com.example.v_sat_compass.ui.exam.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.databinding.ItemExamBinding;

import java.util.ArrayList;
import java.util.List;

public class ExamAdapter extends RecyclerView.Adapter<ExamAdapter.ExamViewHolder> {

    private final List<Exam> exams = new ArrayList<>();
    private OnExamClickListener listener;

    public interface OnExamClickListener {
        void onStartExam(Exam exam);
    }

    public void setOnExamClickListener(OnExamClickListener listener) {
        this.listener = listener;
    }

    public void setExams(List<Exam> newExams) {
        exams.clear();
        exams.addAll(newExams);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemExamBinding binding = ItemExamBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ExamViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ExamViewHolder holder, int position) {
        holder.bind(exams.get(position));
    }

    @Override
    public int getItemCount() {
        return exams.size();
    }

    class ExamViewHolder extends RecyclerView.ViewHolder {
        private final ItemExamBinding binding;

        ExamViewHolder(ItemExamBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Exam exam) {
            binding.tvExamTitle.setText(exam.getTitle());
            binding.tvSubjectName.setText(exam.getSubjectName() != null ? exam.getSubjectName() : "");
            binding.tvQuestionCount.setText(exam.getTotalQuestions() + " cau");
            binding.tvDuration.setText(exam.getDurationMinutes() + " phut");

            binding.btnStartExam.setOnClickListener(v -> {
                if (listener != null) listener.onStartExam(exam);
            });
        }
    }
}
