package com.example.v_sat_compass.util;

import com.example.v_sat_compass.data.model.proctoring.ProctoringViolationLog;
import com.example.v_sat_compass.data.model.proctoring.ProctoringViolationType;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ProctoringMonitorTest {

    private ProctoringMonitor monitor;

    @Before
    public void setUp() {
        monitor = new ProctoringMonitor(true);
    }

    @Test
    public void testInitialState() {
        assertTrue(monitor.isProctoringActive());
        assertEquals(0, monitor.getViolationCount());
        assertFalse(monitor.shouldAutoSubmit());
        assertTrue(monitor.getViolationLogs().isEmpty());
    }

    @Test
    public void testViolationCountIncrementAndAutoSubmitTrigger() {
        // Violation 1: FOCUS_LOST
        int count1 = monitor.recordViolation(ProctoringViolationType.FOCUS_LOST, "Lost window focus");
        assertEquals(1, count1);
        assertEquals(1, monitor.getViolationCount());
        assertFalse(monitor.shouldAutoSubmit());

        // Violation 2: APP_BACKGROUNDED
        int count2 = monitor.recordViolation(ProctoringViolationType.APP_BACKGROUNDED, "User switched app");
        assertEquals(2, count2);
        assertEquals(2, monitor.getViolationCount());
        assertFalse(monitor.shouldAutoSubmit());

        // Violation 3: MULTI_WINDOW_DETECTED -> Threshold reached!
        int count3 = monitor.recordViolation(ProctoringViolationType.MULTI_WINDOW_DETECTED, "Split screen detected");
        assertEquals(3, count3);
        assertEquals(3, monitor.getViolationCount());
        assertTrue(monitor.shouldAutoSubmit());

        // Further violations still maintain shouldAutoSubmit == true
        int count4 = monitor.recordViolation(ProctoringViolationType.SCREEN_OVERLAY, "Overlay detected");
        assertEquals(4, count4);
        assertTrue(monitor.shouldAutoSubmit());
    }

    @Test
    public void testViolationLogDetails() {
        long beforeTime = System.currentTimeMillis();
        monitor.recordViolation(ProctoringViolationType.FOCUS_LOST, "Detail 1");
        monitor.recordViolation(ProctoringViolationType.MULTI_WINDOW_DETECTED, "Detail 2");
        long afterTime = System.currentTimeMillis();

        List<ProctoringViolationLog> logs = monitor.getViolationLogs();
        assertEquals(2, logs.size());

        ProctoringViolationLog log1 = logs.get(0);
        assertEquals(ProctoringViolationType.FOCUS_LOST, log1.getType());
        assertEquals("Detail 1", log1.getDescription());
        assertEquals(1, log1.getViolationNumber());
        assertTrue(log1.getTimestamp() >= beforeTime && log1.getTimestamp() <= afterTime);

        ProctoringViolationLog log2 = logs.get(1);
        assertEquals(ProctoringViolationType.MULTI_WINDOW_DETECTED, log2.getType());
        assertEquals("Detail 2", log2.getDescription());
        assertEquals(2, log2.getViolationNumber());
        assertTrue(log2.getTimestamp() >= beforeTime && log2.getTimestamp() <= afterTime);
    }

    @Test
    public void testResetClearsViolations() {
        monitor.recordViolation(ProctoringViolationType.FOCUS_LOST);
        monitor.recordViolation(ProctoringViolationType.APP_BACKGROUNDED);
        monitor.recordViolation(ProctoringViolationType.MULTI_WINDOW_DETECTED);
        assertTrue(monitor.shouldAutoSubmit());

        monitor.reset();
        assertEquals(0, monitor.getViolationCount());
        assertFalse(monitor.shouldAutoSubmit());
        assertTrue(monitor.getViolationLogs().isEmpty());
    }

    @Test
    public void testSetProctoringActive() {
        ProctoringMonitor inactiveMonitor = new ProctoringMonitor(false);
        assertFalse(inactiveMonitor.isProctoringActive());

        inactiveMonitor.setProctoringActive(true);
        assertTrue(inactiveMonitor.isProctoringActive());
    }
}
