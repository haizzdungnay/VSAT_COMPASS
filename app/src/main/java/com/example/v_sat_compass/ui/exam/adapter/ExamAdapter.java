package com.example.v_sat_compass.ui.exam.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.v_sat_compass.R;
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

    public ExamAdapter() {
    }

    public ExamAdapter(List<Exam> initialExams, OnExamClickListener listener) {
        this.exams.addAll(initialExams);
        this.listener = listener;
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
            binding.tvQuestionCount.setText(exam.getTotalQuestions() + " câu");
            binding.tvDuration.setText(exam.getDurationMinutes() + " phút");

            // Price display: passingScore == 0 means free
            double passingScore = exam.getPassingScore();
            if (passingScore <= 0) {
                binding.tvSubjectName.setText("Đề miễn phí");
                binding.tvSubjectName.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.success));
                binding.ivPriceIcon.setImageResource(R.drawable.ic_check_circle);
                binding.ivPriceIcon.setColorFilter(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.success));
            } else {
                String subjectName = exam.getSubjectName() != null ? exam.getSubjectName() : "";
                binding.tvSubjectName.setText(subjectName.isEmpty() ? "30.000đ" : subjectName);
                binding.tvSubjectName.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.text_secondary));
                binding.ivPriceIcon.setImageResource(R.drawable.ic_bookmark);
                binding.ivPriceIcon.setColorFilter(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.text_secondary));
            }

            binding.btnStartExam.setOnClickListener(v -> {
                if (listener != null) listener.onStartExam(exam);
            });

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onStartExam(exam);
            });
        }
    }
}
