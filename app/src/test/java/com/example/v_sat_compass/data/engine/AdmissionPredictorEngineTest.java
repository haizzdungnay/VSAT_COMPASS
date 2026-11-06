package com.example.v_sat_compass.data.engine;

import com.example.v_sat_compass.data.model.admission.AdmissionBenchmarkItem;
import com.example.v_sat_compass.data.model.admission.AdmissionPredictionResult;
import com.example.v_sat_compass.data.model.admission.AdmissionPredictionResult.Tier;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class AdmissionPredictorEngineTest {

    private static final double EPSILON = 0.0001;

    @Test
    public void testClassify_SafeTier() {
        double cutoff = 100.0;
        // score >= cutoff + 15.0 -> SAFE
        assertEquals(Tier.SAFE, AdmissionPredictorEngine.classify(115.0, cutoff));
        assertEquals(Tier.SAFE, AdmissionPredictorEngine.classify(120.0, cutoff));
        assertEquals(Tier.SAFE, AdmissionPredictorEngine.classify(150.0, cutoff));
    }

    @Test
    public void testClassify_FitTier() {
        double cutoff = 100.0;
        // score in [cutoff - 10.0, cutoff + 15.0) -> FIT
        assertEquals(Tier.FIT, AdmissionPredictorEngine.classify(114.99, cutoff));
        assertEquals(Tier.FIT, AdmissionPredictorEngine.classify(100.0, cutoff));
        assertEquals(Tier.FIT, AdmissionPredictorEngine.classify(90.0, cutoff));
    }

    @Test
    public void testClassify_RiskTier() {
        double cutoff = 100.0;
        // score < cutoff - 10.0 -> RISK
        assertEquals(Tier.RISK, AdmissionPredictorEngine.classify(89.99, cutoff));
        assertEquals(Tier.RISK, AdmissionPredictorEngine.classify(80.0, cutoff));
        assertEquals(Tier.RISK, AdmissionPredictorEngine.classify(0.0, cutoff));
    }

    @Test
    public void testClassify_ExactBoundaryThresholds() {
        double cutoff = 95.0;

        // Exactly at +15.0
        assertEquals(Tier.SAFE, AdmissionPredictorEngine.classify(110.0, cutoff));

        // Just below +15.0
        assertEquals(Tier.FIT, AdmissionPredictorEngine.classify(109.99, cutoff));

        // Exactly at -10.0
        assertEquals(Tier.FIT, AdmissionPredictorEngine.classify(85.0, cutoff));

        // Just below -10.0
        assertEquals(Tier.RISK, AdmissionPredictorEngine.classify(84.99, cutoff));
    }

    @Test
    public void testPredictSingle_DeltaCalculation() {
        AdmissionBenchmarkItem item = new AdmissionBenchmarkItem(
                1L, "HUB", "ĐH Ngân hàng", "MIEN_NAM", "7340201",
                "Tài chính - Ngân hàng", "A00", 2026, 95.0, "Ghi chú");

        double score = 112.5;
        AdmissionPredictionResult result = AdmissionPredictorEngine.predictSingle(score, item);

        assertNotNull(result);
        assertEquals(item, result.getBenchmark());
        assertEquals(score, result.getUserScore(), EPSILON);
        assertEquals(17.5, result.getDelta(), EPSILON);
        assertEquals(Tier.SAFE, result.getTier());
    }

    @Test
    public void testPredict_SubjectComboFiltering() {
        List<AdmissionBenchmarkItem> defaults = AdmissionPredictorEngine.getDefaultBenchmarks();

        List<AdmissionPredictionResult> a00Results = AdmissionPredictorEngine.predict(100.0, "A00", null, defaults);
        assertFalse(a00Results.isEmpty());
        for (AdmissionPredictionResult res : a00Results) {
            assertEquals("A00", res.getBenchmark().getSubjectCombo());
        }

        List<AdmissionPredictionResult> b00Results = AdmissionPredictorEngine.predict(100.0, "B00", null, defaults);
        assertFalse(b00Results.isEmpty());
        for (AdmissionPredictionResult res : b00Results) {
            assertEquals("B00", res.getBenchmark().getSubjectCombo());
        }
    }

    @Test
    public void testPredict_UniversityFiltering() {
        List<AdmissionBenchmarkItem> defaults = AdmissionPredictorEngine.getDefaultBenchmarks();

        List<AdmissionPredictionResult> hubResults = AdmissionPredictorEngine.predict(100.0, null, "HUB", defaults);
        assertFalse(hubResults.isEmpty());
        for (AdmissionPredictionResult res : hubResults) {
            assertEquals("HUB", res.getBenchmark().getUniversityCode());
        }

        List<AdmissionPredictionResult> uelResults = AdmissionPredictorEngine.predict(100.0, null, "UEL", defaults);
        assertFalse(uelResults.isEmpty());
        for (AdmissionPredictionResult res : uelResults) {
            assertEquals("UEL", res.getBenchmark().getUniversityCode());
        }
    }

    @Test
    public void testDefaultBenchmarks_ContainsRequiredUniversities() {
        List<AdmissionBenchmarkItem> benchmarks = AdmissionPredictorEngine.getDefaultBenchmarks();
        assertNotNull(benchmarks);
        assertFalse(benchmarks.isEmpty());

        Set<String> uniCodes = new HashSet<>();
        for (AdmissionBenchmarkItem item : benchmarks) {
            uniCodes.add(item.getUniversityCode());
            assertNotNull(item.getMajorCode());
            assertNotNull(item.getMajorName());
            assertNotNull(item.getSubjectCombo());
            assertTrue(item.getCutoffScore() > 0.0);
        }

        // Must provide default benchmarks for HUB, UEL, CTU, UFM, HCMUS, SGU
        assertTrue(uniCodes.contains("HUB"));
        assertTrue(uniCodes.contains("UEL"));
        assertTrue(uniCodes.contains("CTU"));
        assertTrue(uniCodes.contains("UFM"));
        assertTrue(uniCodes.contains("HCMUS"));
        assertTrue(uniCodes.contains("SGU"));
    }

    @Test
    public void testFilterBenchmarks_RegionAndCombo() {
        List<AdmissionBenchmarkItem> defaults = AdmissionPredictorEngine.getDefaultBenchmarks();
        List<AdmissionBenchmarkItem> mienTay = AdmissionPredictorEngine.filterBenchmarks(
                defaults, "A00", "CTU", "MIEN_TAY");

        assertFalse(mienTay.isEmpty());
        for (AdmissionBenchmarkItem item : mienTay) {
            assertEquals("MIEN_TAY", item.getRegion());
            assertEquals("CTU", item.getUniversityCode());
            assertEquals("A00", item.getSubjectCombo());
        }
    }
}
