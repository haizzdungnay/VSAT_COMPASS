package com.example.v_sat_compass.ui.progress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.v_sat_compass.data.model.ExamHistoryEntry;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ProgressAnalyticsTest {

    @Test
    public void hasEnoughData_requiresAtLeastTwoEntries() {
        assertFalse(ProgressActivity.hasEnoughData(null));
        assertFalse(ProgressActivity.hasEnoughData(Collections.emptyList()));
        assertFalse(ProgressActivity.hasEnoughData(Collections.singletonList(createEntry("Math", 25, 20, 1200))));
        assertTrue(ProgressActivity.hasEnoughData(Arrays.asList(
                createEntry("Math", 25, 20, 1200),
                createEntry("Math", 25, 22, 1100)
        )));
    }

    @Test
    public void aggregateBySubject_calculatesAveragesAccurately() {
        // Entry 1: Math, 25 questions, 20 correct (80% -> score 960), 1000 sec
        // Entry 2: Math, 25 questions, 25 correct (100% -> score 1200), 1500 sec
        // Entry 3: English, 20 questions, 18 correct (90% -> score 1080), 900 sec
        List<ExamHistoryEntry> entries = Arrays.asList(
                createEntry("Math", 25, 20, 1000),
                createEntry("Math", 25, 25, 1500),
                createEntry("English", 20, 18, 900)
        );

        Map<String, ProgressActivity.SubjectAggregate> map = ProgressActivity.aggregateBySubject(entries);
        assertEquals(2, map.size());

        ProgressActivity.SubjectAggregate math = map.get("Math");
        assertEquals(2, math.count);
        assertEquals(1080, math.averageScore()); // (960 + 1200) / 2 = 1080
        assertEquals(90, math.averageAccuracy()); // 45 / 50 = 90%
        assertEquals(50, math.averageSecondsPerQuestion()); // 2500 / 50 = 50 sec

        ProgressActivity.SubjectAggregate eng = map.get("English");
        assertEquals(1, eng.count);
        assertEquals(1080, eng.averageScore());
        assertEquals(90, eng.averageAccuracy());
        assertEquals(45, eng.averageSecondsPerQuestion());
    }

    @Test
    public void calculateAvgTimePerQuestion_handlesEdgeCases() {
        assertEquals("--", ProgressActivity.calculateAvgTimePerQuestion(null));
        assertEquals("--", ProgressActivity.calculateAvgTimePerQuestion(Collections.emptyList()));

        List<ExamHistoryEntry> entries = Collections.singletonList(createEntry("Physics", 20, 15, 600));
        assertEquals("30", ProgressActivity.calculateAvgTimePerQuestion(entries));
    }

    @Test
    public void normalizeSubject_defaultsEmptyOrNullToGeneral() {
        assertEquals("General", ProgressActivity.normalizeSubject(null));
        assertEquals("General", ProgressActivity.normalizeSubject("   "));
        assertEquals("Literature", ProgressActivity.normalizeSubject(" Literature "));
    }

    private ExamHistoryEntry createEntry(String subject, int total, int correct, long timeSeconds) {
        double pct = total > 0 ? (correct * 100.0) / total : 0;
        return new ExamHistoryEntry(1L, "Exam", subject, total, correct, pct, timeSeconds, "{}");
    }
}
