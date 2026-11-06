package com.example.v_sat_compass.data.engine;

import com.example.v_sat_compass.data.model.admission.AdmissionBenchmarkItem;
import com.example.v_sat_compass.data.model.admission.AdmissionPredictionResult;
import com.example.v_sat_compass.data.model.admission.AdmissionPredictionResult.Tier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Engine for matching student V-SAT scores against university benchmark cutoffs.
 * Implements standard tiers:
 * - SAFE: score >= cutoff + 15.0
 * - FIT:  score >= cutoff - 10.0 && score < cutoff + 15.0
 * - RISK: score < cutoff - 10.0
 */
public class AdmissionPredictorEngine {

    public static final double SAFE_MARGIN = 15.0;
    public static final double FIT_MARGIN = -10.0;

    /**
     * Classifies a score relative to cutoff.
     */
    public static Tier classify(double score, double cutoffScore) {
        if (score >= cutoffScore + SAFE_MARGIN) {
            return Tier.SAFE;
        } else if (score >= cutoffScore + FIT_MARGIN) {
            return Tier.FIT;
        } else {
            return Tier.RISK;
        }
    }

    /**
     * Predict single benchmark item.
     */
    public static AdmissionPredictionResult predictSingle(double userScore, AdmissionBenchmarkItem benchmark) {
        double cutoff = benchmark != null && benchmark.getCutoffScore() != null ? benchmark.getCutoffScore() : 0.0;
        double delta = userScore - cutoff;
        Tier tier = classify(userScore, cutoff);
        return new AdmissionPredictionResult(benchmark, userScore, delta, tier);
    }

    /**
     * Predict over a list of benchmarks with optional combo and university filters.
     */
    public static List<AdmissionPredictionResult> predict(
            double userScore,
            String subjectCombo,
            String universityCode,
            List<AdmissionBenchmarkItem> benchmarks
    ) {
        if (benchmarks == null) return Collections.emptyList();
        List<AdmissionPredictionResult> results = new ArrayList<>();
        for (AdmissionBenchmarkItem item : benchmarks) {
            if (subjectCombo != null && !subjectCombo.trim().isEmpty() && !subjectCombo.equalsIgnoreCase("ALL")) {
                if (item.getSubjectCombo() == null || !item.getSubjectCombo().equalsIgnoreCase(subjectCombo.trim())) {
                    continue;
                }
            }
            if (universityCode != null && !universityCode.trim().isEmpty() && !universityCode.equalsIgnoreCase("ALL")) {
                if (item.getUniversityCode() == null || !item.getUniversityCode().equalsIgnoreCase(universityCode.trim())) {
                    continue;
                }
            }
            results.add(predictSingle(userScore, item));
        }
        return results;
    }

    /**
     * Filter benchmarks by combo, university and region.
     */
    public static List<AdmissionBenchmarkItem> filterBenchmarks(
            List<AdmissionBenchmarkItem> benchmarks,
            String subjectCombo,
            String universityCode,
            String region
    ) {
        if (benchmarks == null) return Collections.emptyList();
        List<AdmissionBenchmarkItem> filtered = new ArrayList<>();
        for (AdmissionBenchmarkItem item : benchmarks) {
            if (subjectCombo != null && !subjectCombo.trim().isEmpty() && !subjectCombo.equalsIgnoreCase("ALL")) {
                if (item.getSubjectCombo() == null || !item.getSubjectCombo().equalsIgnoreCase(subjectCombo.trim())) {
                    continue;
                }
            }
            if (universityCode != null && !universityCode.trim().isEmpty() && !universityCode.equalsIgnoreCase("ALL")) {
                if (item.getUniversityCode() == null || !item.getUniversityCode().equalsIgnoreCase(universityCode.trim())) {
                    continue;
                }
            }
            if (region != null && !region.trim().isEmpty() && !region.equalsIgnoreCase("ALL")) {
                if (item.getRegion() == null || !item.getRegion().equalsIgnoreCase(region.trim())) {
                    continue;
                }
            }
            filtered.add(item);
        }
        return filtered;
    }

    /**
     * Default benchmarks for HUB, UEL, CTU, UFM, HCMUS, SGU.
     */
    public static List<AdmissionBenchmarkItem> getDefaultBenchmarks() {
        List<AdmissionBenchmarkItem> list = new ArrayList<>();

        // HUB (Đại học Ngân hàng TP.HCM)
        list.add(new AdmissionBenchmarkItem(1L, "HUB", "Đại học Ngân hàng TP.HCM", "MIEN_NAM", "7340201", "Tài chính - Ngân hàng", "A00", 2026, 95.0, "V-SAT chuẩn đầu vào"));
        list.add(new AdmissionBenchmarkItem(2L, "HUB", "Đại học Ngân hàng TP.HCM", "MIEN_NAM", "7340201", "Tài chính - Ngân hàng", "A01", 2026, 95.0, "V-SAT chuẩn đầu vào"));
        list.add(new AdmissionBenchmarkItem(3L, "HUB", "Đại học Ngân hàng TP.HCM", "MIEN_NAM", "7340201", "Tài chính - Ngân hàng", "D01", 2026, 92.5, "V-SAT chuẩn đầu vào"));
        list.add(new AdmissionBenchmarkItem(4L, "HUB", "Đại học Ngân hàng TP.HCM", "MIEN_NAM", "7340101", "Quản trị kinh doanh", "A00", 2026, 90.0, "V-SAT chuẩn đầu vào"));
        list.add(new AdmissionBenchmarkItem(5L, "HUB", "Đại học Ngân hàng TP.HCM", "MIEN_NAM", "7340101", "Quản trị kinh doanh", "D01", 2026, 88.0, "V-SAT chuẩn đầu vào"));

        // UEL (Đại học Kinh tế - Luật ĐHQG-HCM)
        list.add(new AdmissionBenchmarkItem(6L, "UEL", "Trường ĐH Kinh tế - Luật (ĐHQG-HCM)", "MIEN_NAM", "7310106", "Kinh tế quốc tế", "A00", 2026, 105.0, "V-SAT chuẩn đầu vào"));
        list.add(new AdmissionBenchmarkItem(7L, "UEL", "Trường ĐH Kinh tế - Luật (ĐHQG-HCM)", "MIEN_NAM", "7310106", "Kinh tế quốc tế", "A01", 2026, 105.0, "V-SAT chuẩn đầu vào"));
        list.add(new AdmissionBenchmarkItem(8L, "UEL", "Trường ĐH Kinh tế - Luật (ĐHQG-HCM)", "MIEN_NAM", "7310106", "Kinh tế quốc tế", "D01", 2026, 102.0, "V-SAT chuẩn đầu vào"));
        list.add(new AdmissionBenchmarkItem(9L, "UEL", "Trường ĐH Kinh tế - Luật (ĐHQG-HCM)", "MIEN_NAM", "7380107", "Luật kinh tế", "C00", 2026, 98.0, "V-SAT chuẩn đầu vào"));
        list.add(new AdmissionBenchmarkItem(10L, "UEL", "Trường ĐH Kinh tế - Luật (ĐHQG-HCM)", "MIEN_NAM", "7380107", "Luật kinh tế", "D01", 2026, 96.0, "V-SAT chuẩn đầu vào"));

        // CTU (Đại học Cần Thơ)
        list.add(new AdmissionBenchmarkItem(11L, "CTU", "Đại học Cần Thơ", "MIEN_TAY", "7480201", "Công nghệ thông tin", "A00", 2026, 92.0, "V-SAT chuẩn đầu vào"));
        list.add(new AdmissionBenchmarkItem(12L, "CTU", "Đại học Cần Thơ", "MIEN_TAY", "7480201", "Công nghệ thông tin", "A01", 2026, 90.0, "V-SAT chuẩn đầu vào"));
        list.add(new AdmissionBenchmarkItem(13L, "CTU", "Đại học Cần Thơ", "MIEN_TAY", "7620105", "Nông nghiệp công nghệ cao", "B00", 2026, 80.0, "V-SAT chuẩn đầu vào"));
        list.add(new AdmissionBenchmarkItem(14L, "CTU", "Đại học Cần Thơ", "MIEN_TAY", "7140209", "Sư phạm Toán học", "A00", 2026, 96.0, "V-SAT chuẩn đầu vào"));

        // UFM (Trường ĐH Tài chính - Marketing)
        list.add(new AdmissionBenchmarkItem(15L, "UFM", "Trường ĐH Tài chính - Marketing", "MIEN_NAM", "7340115", "Marketing", "A00", 2026, 92.0, "V-SAT chuẩn đầu vào"));
        list.add(new AdmissionBenchmarkItem(16L, "UFM", "Trường ĐH Tài chính - Marketing", "MIEN_NAM", "7340115", "Marketing", "A01", 2026, 92.0, "V-SAT chuẩn đầu vào"));
        list.add(new AdmissionBenchmarkItem(17L, "UFM", "Trường ĐH Tài chính - Marketing", "MIEN_NAM", "7340115", "Marketing", "D01", 2026, 90.0, "V-SAT chuẩn đầu vào"));
        list.add(new AdmissionBenchmarkItem(18L, "UFM", "Trường ĐH Tài chính - Marketing", "MIEN_NAM", "7340205", "Bất động sản", "A00", 2026, 82.0, "V-SAT chuẩn đầu vào"));

        // HCMUS (Trường ĐH Khoa học Tự nhiên ĐHQG-HCM)
        list.add(new AdmissionBenchmarkItem(19L, "HCMUS", "Trường ĐH Khoa học Tự nhiên (ĐHQG-HCM)", "MIEN_NAM", "7480101", "Khoa học máy tính", "A00", 2026, 110.0, "V-SAT chuẩn đầu vào"));
        list.add(new AdmissionBenchmarkItem(20L, "HCMUS", "Trường ĐH Khoa học Tự nhiên (ĐHQG-HCM)", "MIEN_NAM", "7480101", "Khoa học máy tính", "A01", 2026, 110.0, "V-SAT chuẩn đầu vào"));
        list.add(new AdmissionBenchmarkItem(21L, "HCMUS", "Trường ĐH Khoa học Tự nhiên (ĐHQG-HCM)", "MIEN_NAM", "7440112", "Hóa học", "A00", 2026, 85.0, "V-SAT chuẩn đầu vào"));
        list.add(new AdmissionBenchmarkItem(22L, "HCMUS", "Trường ĐH Khoa học Tự nhiên (ĐHQG-HCM)", "MIEN_NAM", "7440112", "Hóa học", "B00", 2026, 88.0, "V-SAT chuẩn đầu vào"));
        list.add(new AdmissionBenchmarkItem(23L, "HCMUS", "Trường ĐH Khoa học Tự nhiên (ĐHQG-HCM)", "MIEN_NAM", "7420201", "Công nghệ sinh học", "B00", 2026, 87.0, "V-SAT chuẩn đầu vào"));

        // SGU (Trường Đại học Sài Gòn)
        list.add(new AdmissionBenchmarkItem(24L, "SGU", "Trường Đại học Sài Gòn", "MIEN_NAM", "7140231", "Sư phạm Tiếng Anh", "D01", 2026, 100.0, "V-SAT chuẩn đầu vào"));
        list.add(new AdmissionBenchmarkItem(25L, "SGU", "Trường Đại học Sài Gòn", "MIEN_NAM", "7480201", "Công nghệ thông tin", "A00", 2026, 94.0, "V-SAT chuẩn đầu vào"));
        list.add(new AdmissionBenchmarkItem(26L, "SGU", "Trường Đại học Sài Gòn", "MIEN_NAM", "7480201", "Công nghệ thông tin", "A01", 2026, 93.0, "V-SAT chuẩn đầu vào"));
        list.add(new AdmissionBenchmarkItem(27L, "SGU", "Trường Đại học Sài Gòn", "MIEN_NAM", "7340101", "Quản trị kinh doanh", "D01", 2026, 88.0, "V-SAT chuẩn đầu vào"));

        return list;
    }
}
