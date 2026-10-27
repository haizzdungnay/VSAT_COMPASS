package com.example.v_sat_compass.ui.exam.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.databinding.ItemExamBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExamAdapter extends RecyclerView.Adapter<ExamAdapter.ExamViewHolder> {

    private final List<Exam> exams = new ArrayList<>();
    private OnExamClickListener listener;
    private OnExamDownloadClickListener downloadListener;
    private final Set<Long> downloadedExamIds = new HashSet<>();
    private final Set<Long> downloadingExamIds = new HashSet<>();

    public interface OnExamClickListener {
        void onStartExam(Exam exam);
    }

    public interface OnExamDownloadClickListener {
        void onDownloadExam(Exam exam);
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

    public void setOnExamDownloadClickListener(OnExamDownloadClickListener listener) {
        this.downloadListener = listener;
    }

    public void setDownloadedExamIds(Set<Long> ids) {
        downloadedExamIds.clear();
        if (ids != null) downloadedExamIds.addAll(ids);
        notifyDataSetChanged();
    }

    public void setDownloading(long examId, boolean downloading) {
        if (downloading) {
            downloadingExamIds.add(examId);
        } else {
            downloadingExamIds.remove(examId);
        }
        notifyDataSetChanged();
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

            String subjectName = exam.getSubjectName() != null ? exam.getSubjectName() : "";
            boolean isFree = isFreeExam(exam);
            if (isFree) {
                String label = subjectName.isEmpty()
                        ? binding.getRoot().getContext().getString(R.string.exam_badge_free)
                        : subjectName + " · " + binding.getRoot().getContext().getString(R.string.exam_badge_free);
                binding.tvSubjectName.setText(label);
                binding.tvSubjectName.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.success));
                binding.ivPriceIcon.setImageResource(R.drawable.ic_check_circle);
                binding.ivPriceIcon.setColorFilter(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.success));
            } else {
                binding.tvSubjectName.setText(subjectName.isEmpty()
                        ? exam.getPricingType() : subjectName);
                binding.tvSubjectName.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.text_secondary));
                binding.ivPriceIcon.setImageResource(R.drawable.ic_bookmark);
                binding.ivPriceIcon.setColorFilter(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.text_secondary));
            }

            Long examId = exam.getId();
            boolean isDownloaded = examId != null && downloadedExamIds.contains(examId);
            boolean isDownloading = examId != null && downloadingExamIds.contains(examId);
            binding.btnDownloadExam.setVisibility(isFree ? View.VISIBLE : View.GONE);
            binding.btnDownloadExam.setEnabled(!isDownloaded && !isDownloading);
            if (isDownloading) {
                binding.btnDownloadExam.setText("Đang tải");
            } else if (isDownloaded) {
                binding.btnDownloadExam.setText("Đã tải");
            } else {
                binding.btnDownloadExam.setText("Tải xuống");
            }
            binding.btnDownloadExam.setOnClickListener(v -> {
                if (downloadListener != null) downloadListener.onDownloadExam(exam);
            });

            binding.btnStartExam.setOnClickListener(v -> {
                if (listener != null) listener.onStartExam(exam);
            });

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onStartExam(exam);
            });
        }

        private boolean isFreeExam(Exam exam) {
            String pricingType = exam.getPricingType();
            if (pricingType != null) {
                return "FREE".equalsIgnoreCase(pricingType);
            }
            return exam.getPassingScore() <= 0;
        }
    }
}
