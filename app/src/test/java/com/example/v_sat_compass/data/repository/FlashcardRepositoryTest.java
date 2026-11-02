package com.example.v_sat_compass.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.AssetManager;

import com.example.v_sat_compass.data.model.Flashcard;
import com.example.v_sat_compass.data.model.FlashcardProgress;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class FlashcardRepositoryTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File fakeFilesDir;
    private Context fakeContext;
    private AssetManager mockAssets;
    private ExecutorService directExecutor;
    private FlashcardRepository repository;
    private final Gson gson = new Gson();

    private final String sampleSeed = "[{\"id\":\"c1\",\"front\":\"Photosynthesis\",\"back\":\"Process of converting light to energy\",\"deck\":\"Biology\",\"subject\":\"Science\"}," +
            "{\"id\":\"c2\",\"front\":\"Pythagorean theorem\",\"back\":\"a^2 + b^2 = c^2\",\"deck\":\"Geometry\",\"subject\":\"Math\"}]";

    @Before
    public void setUp() throws Exception {
        fakeFilesDir = tempFolder.newFolder("files");
        mockAssets = mock(AssetManager.class);
        fakeContext = mock(Context.class);

        when(fakeContext.getFilesDir()).thenReturn(fakeFilesDir);
        when(fakeContext.getApplicationContext()).thenReturn(fakeContext);
        when(fakeContext.getAssets()).thenReturn(mockAssets);
        when(mockAssets.open("flashcards_seed.json"))
                .thenAnswer(inv -> new ByteArrayInputStream(sampleSeed.getBytes(StandardCharsets.UTF_8)));

        directExecutor = Executors.newSingleThreadExecutor();
        repository = new FlashcardRepository(directExecutor, null, gson);
    }

    private void awaitTasks() {
        try {
            directExecutor.submit(() -> {}).get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getAllCards_loadsSeedAndAppliesDefaultProgress() {
        AtomicReference<List<Flashcard>> ref = new AtomicReference<>();
        repository.getAllCards(fakeContext, ref::set);
        awaitTasks();

        List<Flashcard> cards = ref.get();
        assertNotNull(cards);
        assertEquals(2, cards.size());
        assertEquals("c1", cards.get(0).getId());
        assertEquals("Photosynthesis", cards.get(0).getFront());
        assertEquals("c2", cards.get(1).getId());
    }

    @Test
    public void getDueCards_filtersDueCardsCorrectly() {
        AtomicReference<List<Flashcard>> ref = new AtomicReference<>();
        repository.getDueCards(fakeContext, ref::set);
        awaitTasks();

        List<Flashcard> due = ref.get();
        assertNotNull(due);
        // Brand new cards have nextReviewAtMillis = 0 <= now, so both are due
        assertEquals(2, due.size());
    }

    @Test
    public void review_persistsProgressAndReschedulesCard() {
        AtomicReference<List<Flashcard>> allRef = new AtomicReference<>();
        repository.getAllCards(fakeContext, allRef::set);
        awaitTasks();

        Flashcard card = allRef.get().get(0);
        AtomicReference<Boolean> doneRef = new AtomicReference<>(false);
        repository.review(fakeContext, card, FlashcardScheduler.Rating.GOOD, () -> doneRef.set(true));
        awaitTasks();

        assertTrue(doneRef.get());

        // Verify progress survived in a fresh repository instance
        FlashcardRepository newRepo = new FlashcardRepository(directExecutor, null, gson);
        AtomicReference<List<Flashcard>> reloadedRef = new AtomicReference<>();
        newRepo.getAllCards(fakeContext, reloadedRef::set);
        awaitTasks();

        Flashcard updatedCard = reloadedRef.get().get(0);
        assertEquals("c1", updatedCard.getId());
        assertEquals(1, updatedCard.getIntervalDays());
        assertEquals(1, updatedCard.getRepetitions());
        assertTrue(updatedCard.getNextReviewAtMillis() > System.currentTimeMillis());
    }
}
