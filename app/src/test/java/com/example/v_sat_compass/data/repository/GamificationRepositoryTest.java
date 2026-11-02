package com.example.v_sat_compass.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;

import com.example.v_sat_compass.data.model.GamificationState;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class GamificationRepositoryTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File fakeFilesDir;
    private Context fakeContext;
    private ExecutorService directExecutor;
    private GamificationRepository repository;
    private final Gson gson = new Gson();

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
        directExecutor = Executors.newSingleThreadExecutor();
        repository = new GamificationRepository(directExecutor, null, gson);
    }

    private void awaitTasks() {
        try {
            directExecutor.submit(() -> {}).get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void initialStateIsEmpty() {
        AtomicReference<GamificationState> ref = new AtomicReference<>();
        repository.getState(fakeContext, ref::set);
        awaitTasks();

        GamificationState state = ref.get();
        assertEquals(0, state.getCurrentStreak());
        assertEquals(0, state.getTotalXp());
        assertEquals(0, state.getChallengeProgress());
        assertFalse(state.isChallengeCompleted());
        assertEquals(1, state.getLevel());
    }

    @Test
    public void recordPracticeAnswer_correct_addsXpAndChallengeProgress() {
        repository.recordPracticeAnswer(fakeContext, true);
        awaitTasks();

        AtomicReference<GamificationState> ref = new AtomicReference<>();
        repository.getState(fakeContext, ref::set);
        awaitTasks();

        GamificationState state = ref.get();
        assertEquals(10, state.getTotalXp());
        assertEquals(1, state.getCurrentStreak());
        assertEquals(1, state.getChallengeProgress());
        assertFalse(state.isChallengeCompleted());
    }

    @Test
    public void recordPracticeAnswer_wrong_maintainsXpButUpdatesStreak() {
        repository.recordPracticeAnswer(fakeContext, false);
        awaitTasks();

        AtomicReference<GamificationState> ref = new AtomicReference<>();
        repository.getState(fakeContext, ref::set);
        awaitTasks();

        GamificationState state = ref.get();
        assertEquals(0, state.getTotalXp());
        assertEquals(1, state.getCurrentStreak());
        assertEquals(0, state.getChallengeProgress());
    }

    @Test
    public void recordPracticeAnswer_targetReached_completesChallenge() {
        for (int i = 0; i < 10; i++) {
            repository.recordPracticeAnswer(fakeContext, true);
        }
        awaitTasks();

        AtomicReference<GamificationState> ref = new AtomicReference<>();
        repository.getState(fakeContext, ref::set);
        awaitTasks();

        GamificationState state = ref.get();
        assertEquals(100, state.getTotalXp());
        assertEquals(10, state.getChallengeProgress());
        assertTrue(state.isChallengeCompleted());
    }

    @Test
    public void recordExamCompleted_addsExamAndQuestionXp() {
        repository.recordExamCompleted(fakeContext, 5, 10);
        awaitTasks();

        AtomicReference<GamificationState> ref = new AtomicReference<>();
        repository.getState(fakeContext, ref::set);
        awaitTasks();

        GamificationState state = ref.get();
        // 50 (exam) + 5 * 10 (correct) = 100 XP
        assertEquals(100, state.getTotalXp());
        assertEquals(5, state.getChallengeProgress());
        assertEquals(1, state.getCurrentStreak());
    }

    @Test
    public void recordFlashcardReviewed_addsFiveXp() {
        repository.recordFlashcardReviewed(fakeContext);
        awaitTasks();

        AtomicReference<GamificationState> ref = new AtomicReference<>();
        repository.getState(fakeContext, ref::set);
        awaitTasks();

        GamificationState state = ref.get();
        assertEquals(5, state.getTotalXp());
        assertEquals(1, state.getCurrentStreak());
    }

    @Test
    public void consecutiveDayIncrementsStreak_skippedDayResets() throws Exception {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        GamificationState existing = new GamificationState();
        existing.setLastActivityDate(yesterday.toString());
        existing.setCurrentStreak(3);
        existing.setTotalXp(150);

        File stateFile = new File(fakeFilesDir, "gamification_state.json");
        try (FileWriter writer = new FileWriter(stateFile)) {
            gson.toJson(existing, writer);
        }

        repository.recordPracticeAnswer(fakeContext, true);
        awaitTasks();

        AtomicReference<GamificationState> ref = new AtomicReference<>();
        repository.getState(fakeContext, ref::set);
        awaitTasks();

        GamificationState updated = ref.get();
        assertEquals(4, updated.getCurrentStreak());
        assertEquals(160, updated.getTotalXp());
    }

    @Test
    public void statePersistenceSurvivesNewRepositoryInstance() {
        repository.recordPracticeAnswer(fakeContext, true);
        repository.recordFlashcardReviewed(fakeContext);
        awaitTasks();

        GamificationRepository newRepo = new GamificationRepository(directExecutor, null, gson);
        AtomicReference<GamificationState> ref = new AtomicReference<>();
        newRepo.getState(fakeContext, ref::set);
        awaitTasks();

        GamificationState loaded = ref.get();
        assertEquals(15, loaded.getTotalXp());
        assertEquals(1, loaded.getCurrentStreak());
        assertEquals(1, loaded.getChallengeProgress());
    }
}
