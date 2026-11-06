package com.example.v_sat_compass.ui.admission;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.v_sat_compass.data.engine.AdmissionPredictorEngine;
import com.example.v_sat_compass.data.model.admission.AdmissionBenchmarkItem;
import com.example.v_sat_compass.data.model.admission.AdmissionPredictionResult;
import com.example.v_sat_compass.data.model.admission.AdmissionPredictionResult.Tier;
import com.example.v_sat_compass.databinding.ActivityAdmissionPredictorBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity for predicting university admission matching based on V-SAT scores.
 * Categorizes opportunities into 3 color-coded tiers:
 * - SAFE (Green): Score >= cutoff + 15
 * - FIT (Yellow/Orange): Score in [cutoff - 10, cutoff + 15)
 * - RISK (Red): Score < cutoff - 10
 */
public class AdmissionPredictorActivity extends AppCompatActivity {

    private ActivityAdmissionPredictorBinding binding;
    private AdmissionPredictionAdapter adapter;
    private List<AdmissionBenchmarkItem> benchmarks;
    private List<AdmissionPredictionResult> allPredictions = new ArrayList<>();
    private Tier currentTierFilter = null; // null = all

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdmissionPredictorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        benchmarks = AdmissionPredictorEngine.getDefaultBenchmarks();

        setupUI();
        initSpinners();
        performPrediction();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

        adapter = new AdmissionPredictionAdapter();
        binding.rvPredictions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPredictions.setAdapter(adapter);

        double initialScore = getIntent().getDoubleExtra("vsat_score", 100.0);
        binding.etVsatScore.setText(String.format(Locale.getDefault(), "%.1f", initialScore));

        binding.btnPredict.setOnClickListener(v -> performPrediction());

        binding.cardFilterSafe.setOnClickListener(v -> toggleTierFilter(Tier.SAFE));
        binding.cardFilterFit.setOnClickListener(v -> toggleTierFilter(Tier.FIT));
        binding.cardFilterRisk.setOnClickListener(v -> toggleTierFilter(Tier.RISK));
    }

    private void initSpinners() {
        String[] combos = {"Tất cả", "A00", "A01", "D01", "B00", "C00"};
        ArrayAdapter<String> comboAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, combos);
        binding.spinnerSubjectCombo.setAdapter(comboAdapter);

        String[] universities = {"Tất cả", "HUB", "UEL", "CTU", "UFM", "HCMUS", "SGU"};
        ArrayAdapter<String> uniAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, universities);
        binding.spinnerUniversity.setAdapter(uniAdapter);
    }

    private void toggleTierFilter(Tier tier) {
        if (currentTierFilter == tier) {
            currentTierFilter = null;
        } else {
            currentTierFilter = tier;
        }
        applyCurrentFilter();
    }

    private void performPrediction() {
        String scoreStr = binding.etVsatScore.getText() != null
                ? binding.etVsatScore.getText().toString().trim()
                : "";

        double score = 100.0;
        if (!scoreStr.isEmpty()) {
            try {
                score = Double.parseDouble(scoreStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Vui lòng nhập điểm số hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String combo = binding.spinnerSubjectCombo.getSelectedItem() != null
                ? binding.spinnerSubjectCombo.getSelectedItem().toString()
                : "Tất cả";
        String uni = binding.spinnerUniversity.getSelectedItem() != null
                ? binding.spinnerUniversity.getSelectedItem().toString()
                : "Tất cả";

        String comboFilter = "Tất cả".equalsIgnoreCase(combo) ? null : combo;
        String uniFilter = "Tất cả".equalsIgnoreCase(uni) ? null : uni;

        allPredictions = AdmissionPredictorEngine.predict(score, comboFilter, uniFilter, benchmarks);

        int safeCount = 0;
        int fitCount = 0;
        int riskCount = 0;

        for (AdmissionPredictionResult res : allPredictions) {
            if (res.getTier() == Tier.SAFE) safeCount++;
            else if (res.getTier() == Tier.FIT) fitCount++;
            else if (res.getTier() == Tier.RISK) riskCount++;
        }

        binding.tvCountSafe.setText(String.valueOf(safeCount));
        binding.tvCountFit.setText(String.valueOf(fitCount));
        binding.tvCountRisk.setText(String.valueOf(riskCount));

        applyCurrentFilter();
    }

    private void applyCurrentFilter() {
        List<AdmissionPredictionResult> filtered = new ArrayList<>();
        for (AdmissionPredictionResult res : allPredictions) {
            if (currentTierFilter == null || res.getTier() == currentTierFilter) {
                filtered.add(res);
            }
        }

        adapter.submitList(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
