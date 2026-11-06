package com.example.v_sat_compass.data.model.admission;

import com.google.gson.annotations.SerializedName;

public class AdmissionPredictionResult {

    public enum Tier {
        SAFE,
        FIT,
        RISK
    }

    @SerializedName("benchmark")
    private AdmissionBenchmarkItem benchmark;

    @SerializedName("userScore")
    private double userScore;

    @SerializedName("delta")
    private double delta;

    @SerializedName("tier")
    private Tier tier;

    public AdmissionPredictionResult() {}

    public AdmissionPredictionResult(AdmissionBenchmarkItem benchmark, double userScore, double delta, Tier tier) {
        this.benchmark = benchmark;
        this.userScore = userScore;
        this.delta = delta;
        this.tier = tier;
    }

    public AdmissionBenchmarkItem getBenchmark() { return benchmark; }
    public void setBenchmark(AdmissionBenchmarkItem benchmark) { this.benchmark = benchmark; }

    public double getUserScore() { return userScore; }
    public void setUserScore(double userScore) { this.userScore = userScore; }

    public double getDelta() { return delta; }
    public void setDelta(double delta) { this.delta = delta; }

    public Tier getTier() { return tier; }
    public void setTier(Tier tier) { this.tier = tier; }
}
