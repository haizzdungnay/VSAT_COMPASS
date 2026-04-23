package com.example.v_sat_compass.ui.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.ExamHistoryEntry;
import com.example.v_sat_compass.util.RelativeTimeHelper;

import java.util.ArrayList;
import java.util.List;

public class ExamHistoryAdapter extends RecyclerView.Adapter<ExamHistoryAdapter.ViewHolder> {

    public interface OnReviewClickListener {
        void onReview(ExamHistoryEntry entry);
    }

    private List<ExamHistoryEntry> entries = new ArrayList<>();
    private OnReviewClickListener listener;

    public void setEntries(List<ExamHistoryEntry> entries) {
        this.entries = entries != null ? entries : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnReviewClickListener(OnReviewClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exam_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExamHistoryEntry entry = entries.get(position);
        android.content.Context ctx = holder.itemView.getContext();

        holder.tvTitle.setText(entry.getExamTitle());
        holder.tvSubject.setText(entry.getSubject());
        holder.tvScore.setText(String.valueOf(entry.getScore()));

        holder.tvDate.setText(RelativeTimeHelper.format(ctx, entry.getSubmittedAtMillis()));

        holder.tvCorrect.setText(ctx.getString(
                R.string.history_correct_count,
                entry.getCorrectCount(), entry.getTotalQuestions()));

        long mins = entry.getTimeSpentSeconds() / 60;
        long secs = entry.getTimeSpentSeconds() % 60;
        holder.tvTime.setText(ctx.getString(R.string.history_time_format, mins, secs));

        holder.btnReview.setOnClickListener(v -> {
            if (listener != null) listener.onReview(entry);
        });
    }

    @Override
    public int getItemCount() { return entries.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubject, tvScore, tvDate, tvCorrect, tvTime;
        Button btnReview;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle   = itemView.findViewById(R.id.tvHistoryTitle);
            tvSubject = itemView.findViewById(R.id.tvHistorySubject);
            tvScore   = itemView.findViewById(R.id.tvHistoryScore);
            tvDate    = itemView.findViewById(R.id.tvHistoryDate);
            tvCorrect = itemView.findViewById(R.id.tvHistoryCorrect);
            tvTime    = itemView.findViewById(R.id.tvHistoryTime);
            btnReview = itemView.findViewById(R.id.btnReview);
        }
    }
}
