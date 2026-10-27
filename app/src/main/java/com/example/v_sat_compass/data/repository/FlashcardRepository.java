package com.example.v_sat_compass.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.v_sat_compass.data.model.Flashcard;
import com.example.v_sat_compass.data.model.FlashcardProgress;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FlashcardRepository {
    private static final String TAG = "FlashcardRepository";
    private static final String SEED_FILE = "flashcards_seed.json";
    private static final String PROGRESS_FILE = "flashcard_progress.json";
    private static final String PROGRESS_FILE_TMP = "flashcard_progress.json.tmp";

    private static FlashcardRepository instance;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Gson gson = new Gson();
    private final Object fileLock = new Object();

    private FlashcardRepository() {}

    public static synchronized FlashcardRepository getInstance() {
        if (instance == null) {
            instance = new FlashcardRepository();
        }
        return instance;
    }

    public interface Callback<T> {
        void onResult(T result);
    }

    public void getDueCards(Context context, Callback<List<Flashcard>> callback) {
        Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            List<Flashcard> due = new ArrayList<>();
            long now = System.currentTimeMillis();
            synchronized (fileLock) {
                for (Flashcard card : loadCardsWithProgressSync(appCtx)) {
                    if (card.getNextReviewAtMillis() <= now) {
                        due.add(card);
                    }
                }
            }
            postOnMain(appCtx, () -> callback.onResult(due));
        });
    }

    public void getAllCards(Context context, Callback<List<Flashcard>> callback) {
        Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            List<Flashcard> cards;
            synchronized (fileLock) {
                cards = loadCardsWithProgressSync(appCtx);
            }
            postOnMain(appCtx, () -> callback.onResult(cards));
        });
    }

    public void review(Context context, Flashcard card, FlashcardScheduler.Rating rating, Runnable onDone) {
        Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            synchronized (fileLock) {
                Map<String, FlashcardProgress> progressById = loadProgressMapSync(appCtx);
                FlashcardProgress progress = FlashcardScheduler.review(card, rating, System.currentTimeMillis());
                progressById.put(progress.getCardId(), progress);
                saveAtomicSync(appCtx, new ArrayList<>(progressById.values()));
            }
            if (onDone != null) postOnMain(appCtx, onDone);
        });
    }

    private List<Flashcard> loadCardsWithProgressSync(Context context) {
        List<Flashcard> cards = loadSeedSync(context);
        Map<String, FlashcardProgress> progressById = loadProgressMapSync(context);
        for (Flashcard card : cards) {
            FlashcardProgress progress = progressById.get(card.getId());
            if (progress != null) {
                card.applyProgress(progress);
            }
        }
        return cards;
    }

    private List<Flashcard> loadSeedSync(Context context) {
        try (InputStreamReader reader = new InputStreamReader(
                context.getAssets().open(SEED_FILE), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<List<Flashcard>>() {}.getType();
            List<Flashcard> cards = gson.fromJson(reader, type);
            return cards != null ? cards : new ArrayList<>();
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            Log.w(TAG, "loadSeedSync failed", e);
            return new ArrayList<>();
        }
    }

    private Map<String, FlashcardProgress> loadProgressMapSync(Context context) {
        Map<String, FlashcardProgress> result = new HashMap<>();
        for (FlashcardProgress progress : loadProgressListSync(context)) {
            if (progress.getCardId() != null && !progress.getCardId().isEmpty()) {
                result.put(progress.getCardId(), progress);
            }
        }
        return result;
    }

    private List<FlashcardProgress> loadProgressListSync(Context context) {
        File file = new File(context.getFilesDir(), PROGRESS_FILE);
        if (!file.exists()) return new ArrayList<>();
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<List<FlashcardProgress>>() {}.getType();
            List<FlashcardProgress> list = gson.fromJson(reader, type);
            return list != null ? list : new ArrayList<>();
        } catch (IOException e) {
            Log.w(TAG, "loadProgressListSync IOException", e);
            return new ArrayList<>();
        } catch (com.google.gson.JsonSyntaxException e) {
            File corrupt = new File(context.getFilesDir(), PROGRESS_FILE + ".corrupt." + System.currentTimeMillis());
            boolean renamed = file.renameTo(corrupt);
            Log.w(TAG, "Corrupt flashcard progress renamed=" + renamed, e);
            return new ArrayList<>();
        }
    }

    private boolean saveAtomicSync(Context context, List<FlashcardProgress> progress) {
        File tmp = new File(context.getFilesDir(), PROGRESS_FILE_TMP);
        File target = new File(context.getFilesDir(), PROGRESS_FILE);
        try (FileOutputStream fos = new FileOutputStream(tmp);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            gson.toJson(progress, writer);
            writer.flush();
            fos.getFD().sync();
        } catch (IOException e) {
            Log.e(TAG, "saveAtomicSync write failed", e);
            return false;
        }
        boolean moved = tmp.renameTo(target);
        if (!moved) Log.e(TAG, "saveAtomicSync rename failed");
        return moved;
    }

    private void postOnMain(Context context, Runnable action) {
        new android.os.Handler(context.getMainLooper()).post(action);
    }
}
