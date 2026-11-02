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
        // Fatal malformed payload error
        assertTrue(repository.isUnrecoverableClientError(422));

        // Critical: 400, 404, 401, 403 MUST NOT be treated as unrecoverable (protects student offline submissions)
        assertFalse(repository.isUnrecoverableClientError(400));
        assertFalse(repository.isUnrecoverableClientError(404));
        assertFalse(repository.isUnrecoverableClientError(401));
        assertFalse(repository.isUnrecoverableClientError(403));

        // Transient errors MUST NOT be dropped
        assertFalse(repository.isUnrecoverableClientError(408));
        assertFalse(repository.isUnrecoverableClientError(429));
        assertFalse(repository.isUnrecoverableClientError(500));
        assertFalse(repository.isUnrecoverableClientError(503));
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
}
