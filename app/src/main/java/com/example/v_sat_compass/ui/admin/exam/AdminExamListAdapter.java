package com.example.v_sat_compass.ui.admin.exam;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.admin.AdminExamSummaryResponse;

import java.util.ArrayList;
import java.util.List;

public class AdminExamListAdapter extends RecyclerView.Adapter<AdminExamListAdapter.ExamViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(AdminExamSummaryResponse exam);
    }

    private final List<AdminExamSummaryResponse> allItems = new ArrayList<>();
    private final List<AdminExamSummaryResponse> filteredItems = new ArrayList<>();
    private String currentFilter = null; // null or empty = All
    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<AdminExamSummaryResponse> items) {
        allItems.clear();
        if (items != null) {
            allItems.addAll(items);
        }
        applyFilter();
    }

    public void appendItems(List<AdminExamSummaryResponse> items) {
        if (items != null) {
            allItems.addAll(items);
        }
        applyFilter();
    }

    public void setStatusFilter(String status) {
        this.currentFilter = status;
        applyFilter();
    }

    public String getCurrentFilter() {
        return currentFilter;
    }

    public AdminExamSummaryResponse getItemAt(int position) {
        if (position >= 0 && position < filteredItems.size()) {
            return filteredItems.get(position);
        }
        return null;
    }

    private void applyFilter() {
        filteredItems.clear();
        if (currentFilter == null || currentFilter.isEmpty()) {
            filteredItems.addAll(allItems);
        } else {
            for (AdminExamSummaryResponse item : allItems) {
                if (currentFilter.equals(item.getStatus())) {
                    filteredItems.add(item);
                }
            }
        }
        try {
            notifyDataSetChanged();
        } catch (Exception ignored) {
            // no-op in unit test environment
        }
    }

    @Override
    public int getItemCount() {
        return filteredItems.size();
    }

    @NonNull
    @Override
    public ExamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_exam_row, parent, false);
        return new ExamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExamViewHolder holder, int position) {
        AdminExamSummaryResponse exam = filteredItems.get(position);
        holder.bind(exam);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(exam);
            }
        });
    }

    static class ExamViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvCode;
        private final TextView tvStatus;
        private final TextView tvVersion;
        private final TextView tvUpdatedAt;

        ExamViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvAdminExamTitle);
            tvCode = itemView.findViewById(R.id.tvAdminExamCode);
            tvStatus = itemView.findViewById(R.id.tvAdminExamStatus);
            tvVersion = itemView.findViewById(R.id.tvAdminExamVersion);
            tvUpdatedAt = itemView.findViewById(R.id.tvAdminExamUpdatedAt);
        }

        void bind(AdminExamSummaryResponse exam) {
            tvTitle.setText(exam.getTitle() != null ? exam.getTitle() : "");
            tvCode.setText(exam.getExamCode() != null ? exam.getExamCode() : "");
            String status = exam.getStatus() != null ? exam.getStatus() : "";
            tvStatus.setText(status);
            tvVersion.setText("v" + (exam.getVersion() != null ? exam.getVersion() : 1));
            tvUpdatedAt.setText(exam.getUpdatedAt() != null ? exam.getUpdatedAt() : "");
        }
    }
}
