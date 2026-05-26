package com.example.v_sat_compass.ui.practice;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.v_sat_compass.data.model.TopicStatsResponse;
import com.example.v_sat_compass.databinding.ItemPracticeTopicBinding;

import java.util.ArrayList;
import java.util.List;

public class PracticeTopicAdapter extends RecyclerView.Adapter<PracticeTopicAdapter.ViewHolder> {

    public interface Listener {
        void onPracticeClick(TopicStatsResponse topic);
    }

    private final List<TopicStatsResponse> items = new ArrayList<>();
    private Listener listener;
    private PracticeFragment.LabelProvider labelProvider;

    public void setItems(List<TopicStatsResponse> topics) {
        items.clear();
        if (topics != null) {
            items.addAll(topics);
        }
        notifyDataSetChanged();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setLabelProvider(PracticeFragment.LabelProvider labelProvider) {
        this.labelProvider = labelProvider;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPracticeTopicBinding binding = ItemPracticeTopicBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPracticeTopicBinding binding;

        ViewHolder(ItemPracticeTopicBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TopicStatsResponse topic) {
            binding.tvTopicName.setText(topic.getTopicName() != null ? topic.getTopicName() : "Chủ đề");
            int pct = topic.getPercentage();
            binding.tvTopicPercent.setText(pct + "% (" + topic.getCorrect() + "/" + topic.getTotal() + ")");
            binding.progressTopic.setProgress(pct);
            if (labelProvider != null) {
                binding.tvTopicLabel.setText(labelProvider.labelFor(pct));
            }
            binding.btnPractice.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPracticeClick(topic);
                }
            });
        }
    }
}
