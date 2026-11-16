package com.example.v_sat_compass.data.repository;

import android.content.Context;
import android.content.ContextWrapper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class OfflineExamSyncRepositoryTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File fakeFilesDir;
    private Context fakeContext;
    private OfflineExamSyncRepository repository;

    @Before
    public void setUp() throws Exception {
        fakeFilesDir = tempFolder.newFolder("files");
        fakeContext = new ContextWrapper(null) {
            @Override
            public File getFilesDir() {
                return fakeFilesDir;
            }

            @Override
            public Context getApplicationContext() {
                return this;
            }
        };
        repository = new OfflineExamSyncRepository(null);
    }

    @Test
    public void unrecoverableClientError_classification_protectsStudentOfflineData() {
        // Non-retryable 4xx go to DLQ (all 4xx except 401, 403, 408, 429)
        assertTrue(repository.isUnrecoverableClientError(422));
        assertTrue(repository.isUnrecoverableClientError(400));
        assertTrue(repository.isUnrecoverableClientError(404));

        // Auth, timeout, and rate-limit errors MUST remain pending for retry
        assertFalse(repository.isUnrecoverableClientError(401));
        assertFalse(repository.isUnrecoverableClientError(403));
        assertFalse(repository.isUnrecoverableClientError(408));
        assertFalse(repository.isUnrecoverableClientError(429));

        // 5xx server errors MUST NOT be dropped
        assertFalse(repository.isUnrecoverableClientError(500));
        assertFalse(repository.isUnrecoverableClientError(503));
    }

    @Test
    public void loadQueueSync_corruptJson_quarantinesFileAndStartsFresh() throws Exception {
        File queueFile = new File(fakeFilesDir, "offline_exam_result_queue.json");
        try (java.io.FileWriter writer = new java.io.FileWriter(queueFile)) {
            writer.write("{corrupt-json-content");
        }
        assertTrue(queueFile.exists());

        List<OfflineExamSyncRepository.QueueItem> loaded = repository.loadQueueSync(fakeContext);
        assertTrue(loaded.isEmpty());
        assertFalse("Original corrupt queue file must be moved away", queueFile.exists());

        File[] quarantined = fakeFilesDir.listFiles((dir, name) ->
                name.startsWith("offline_exam_result_queue.json.corrupt-"));
        assertNotNull(quarantined);
        assertEquals(1, quarantined.length);
    }

    @Test
    public void deadLetterQueue_savesAndLoadsSafely_withoutLosingData() {
        OfflineExamSyncRepository.QueueItem item = OfflineExamSyncRepository.item(
                101L,
                80.0,
                20,
                25,
                1800,
                Collections.singletonList(OfflineExamSyncRepository.answer(1001L, 5001L, 1, false))
        );

        boolean moved = repository.moveToDeadLetterSync(fakeContext, item, "Test fatal 422");
        assertTrue("DLQ write must succeed", moved);

        List<OfflineExamSyncRepository.QueueItem> dlq = repository.loadDeadLetterSync(fakeContext);
        assertEquals(1, dlq.size());
        assertEquals(item.localAttemptId, dlq.get(0).localAttemptId);
        assertEquals(101L, dlq.get(0).examId);
    }

    @Test
    public void syncPendingSync_whenOffline_returnsFalseSafely() {
        boolean synced = repository.syncPendingSync(fakeContext);
        assertFalse("When offline, syncPendingSync must return false without attempting network calls", synced);
    }

    @Test
    public void deadLetterQueue_preservesPendingScoreAndAnswerPayload() {
        java.util.Map<String, Object> tfPayload = new java.util.HashMap<>();
        tfPayload.put("a", true);
        tfPayload.put("b", false);

        OfflineExamSyncRepository.QueueItem item = OfflineExamSyncRepository.item(
                202L,
                null,
                null,
                25,
                900,
                Collections.singletonList(
                        OfflineExamSyncRepository.answer(2001L, 7001L, 1, true, tfPayload)));

        assertTrue(repository.moveToDeadLetterSync(fakeContext, item, "Preserve payload"));
        List<OfflineExamSyncRepository.QueueItem> dlq = repository.loadDeadLetterSync(fakeContext);
        assertEquals(1, dlq.size());
        assertNull(dlq.get(0).score);
        assertNull(dlq.get(0).correctCount);
        assertNotNull(dlq.get(0).answers);
        assertEquals(1, dlq.get(0).answers.size());
        assertNotNull(dlq.get(0).answers.get(0).answerPayload);
        assertEquals(Boolean.TRUE, dlq.get(0).answers.get(0).answerPayload.get("a"));
        assertEquals(Boolean.FALSE, dlq.get(0).answers.get(0).answerPayload.get("b"));
    }
}
