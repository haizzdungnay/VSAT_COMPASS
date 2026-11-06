package com.example.v_sat_compass.ui.admission;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.admission.AdmissionBenchmarkItem;
import com.example.v_sat_compass.data.model.admission.AdmissionPredictionResult;
import com.example.v_sat_compass.data.model.admission.AdmissionPredictionResult.Tier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdmissionPredictionAdapter extends RecyclerView.Adapter<AdmissionPredictionAdapter.ViewHolder> {

    private final List<AdmissionPredictionResult> items = new ArrayList<>();

    public void submitList(List<AdmissionPredictionResult> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admission_prediction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvUniversityName;
        private final TextView tvTierBadge;
        private final TextView tvMajorName;
        private final TextView tvSubjectCombo;
        private final TextView tvCutoffScore;
        private final TextView tvDelta;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUniversityName = itemView.findViewById(R.id.tvUniversityName);
            tvTierBadge = itemView.findViewById(R.id.tvTierBadge);
            tvMajorName = itemView.findViewById(R.id.tvMajorName);
            tvSubjectCombo = itemView.findViewById(R.id.tvSubjectCombo);
            tvCutoffScore = itemView.findViewById(R.id.tvCutoffScore);
            tvDelta = itemView.findViewById(R.id.tvDelta);
        }

        void bind(AdmissionPredictionResult item) {
            if (item == null) return;
            AdmissionBenchmarkItem b = item.getBenchmark();

            String uniText = (b != null ? b.getUniversityName() : "") + " (" + (b != null ? b.getUniversityCode() : "") + ")";
            tvUniversityName.setText(uniText);

            String majorText = (b != null ? b.getMajorName() : "") + " (Mã: " + (b != null ? b.getMajorCode() : "") + ")";
            tvMajorName.setText(majorText);

            String comboText = "Tổ hợp: " + (b != null ? b.getSubjectCombo() : "");
            tvSubjectCombo.setText(comboText);

            double cutoff = b != null && b.getCutoffScore() != null ? b.getCutoffScore() : 0.0;
            tvCutoffScore.setText(String.format(Locale.getDefault(), "Chuẩn: %.1f", cutoff));

            double delta = item.getDelta();
            String deltaStr = (delta >= 0 ? "+" : "") + String.format(Locale.getDefault(), "%.1f", delta);
            tvDelta.setText(deltaStr);

            Tier tier = item.getTier();
            if (tier == Tier.SAFE) {
                tvTierBadge.setText("SAFE - An toàn");
                tvTierBadge.setTextColor(Color.parseColor("#27AE60"));
                tvDelta.setTextColor(Color.parseColor("#27AE60"));
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(Color.parseColor("#E8F8F5"));
                gd.setCornerRadius(12f);
                tvTierBadge.setBackground(gd);
            } else if (tier == Tier.FIT) {
                tvTierBadge.setText("FIT - Vừa sức");
                tvTierBadge.setTextColor(Color.parseColor("#F39C12"));
                tvDelta.setTextColor(Color.parseColor("#F39C12"));
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(Color.parseColor("#FEF9E7"));
                gd.setCornerRadius(12f);
                tvTierBadge.setBackground(gd);
            } else {
                tvTierBadge.setText("RISK - Rủi ro");
                tvTierBadge.setTextColor(Color.parseColor("#E74C3C"));
                tvDelta.setTextColor(Color.parseColor("#E74C3C"));
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(Color.parseColor("#FDEDEC"));
                gd.setCornerRadius(12f);
                tvTierBadge.setBackground(gd);
            }
        }
    }
}
