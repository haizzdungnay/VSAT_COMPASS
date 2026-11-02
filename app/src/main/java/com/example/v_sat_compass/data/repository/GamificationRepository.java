package com.example.v_sat_compass.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.v_sat_compass.data.model.GamificationState;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GamificationRepository {
    private static final String TAG = "GamificationRepository";
    private static final String STATE_FILE = "gamification_state.json";
    private static final String STATE_FILE_TMP = "gamification_state.json.tmp";
    private static final int XP_PER_CORRECT = 10;
    private static final int XP_PER_EXAM = 50;
    private static final int XP_PER_FLASHCARD_REVIEW = 5;
    public static final int DAILY_CHALLENGE_TARGET = 10;

    private static GamificationRepository instance;

    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Gson gson;
    private final Object fileLock = new Object();

    private GamificationRepository() {
        this(Executors.newSingleThreadExecutor(), new Handler(Looper.getMainLooper()), new Gson());
    }

    GamificationRepository(ExecutorService executor, Handler mainHandler, Gson gson) {
        this.executor = executor;
        this.mainHandler = mainHandler;
        this.gson = gson != null ? gson : new Gson();
    }

    public static synchronized GamificationRepository getInstance() {
        if (instance == null) {
            instance = new GamificationRepository();
        }
        return instance;
    }

    public interface Callback<T> {
        void onResult(T result);
    }

    public void getState(Context context, Callback<GamificationState> callback) {
        Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            GamificationState state;
            synchronized (fileLock) {
                state = normalizeForToday(loadSync(appCtx), LocalDate.now());
                saveAtomicSync(appCtx, state);
            }
            postOnMain(appCtx, () -> callback.onResult(state));
        });
    }

    public void recordExamCompleted(Context context, int correctCount, int totalQuestions) {
        Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            synchronized (fileLock) {
                LocalDate today = LocalDate.now();
                GamificationState state = normalizeForToday(loadSync(appCtx), today);
                updateStreak(state, today);
                int safeCorrect = Math.max(0, correctCount);
                state.setTotalXp(Math.max(0, state.getTotalXp()) + XP_PER_EXAM + safeCorrect * XP_PER_CORRECT);
                state.setChallengeProgress(Math.min(
                        DAILY_CHALLENGE_TARGET,
                        Math.max(0, state.getChallengeProgress()) + safeCorrect));
                state.setChallengeCompleted(state.getChallengeProgress() >= DAILY_CHALLENGE_TARGET);
                saveAtomicSync(appCtx, state);
            }
        });
    }

    public void recordFlashcardReviewed(Context context) {
        Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            synchronized (fileLock) {
                LocalDate today = LocalDate.now();
                GamificationState state = normalizeForToday(loadSync(appCtx), today);
                updateStreak(state, today);
                state.setTotalXp(Math.max(0, state.getTotalXp()) + XP_PER_FLASHCARD_REVIEW);
                saveAtomicSync(appCtx, state);
            }
        });
    }

    public void recordPracticeAnswer(Context context, boolean correct) {
        Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            synchronized (fileLock) {
                LocalDate today = LocalDate.now();
                GamificationState state = normalizeForToday(loadSync(appCtx), today);
                updateStreak(state, today);
                if (correct) {
                    state.setTotalXp(Math.max(0, state.getTotalXp()) + XP_PER_CORRECT);
                    state.setChallengeProgress(Math.min(
                            DAILY_CHALLENGE_TARGET,
                            Math.max(0, state.getChallengeProgress()) + 1));
                    state.setChallengeCompleted(state.getChallengeProgress() >= DAILY_CHALLENGE_TARGET);
                }
                saveAtomicSync(appCtx, state);
            }
        });
    }

    private GamificationState normalizeForToday(GamificationState state, LocalDate today) {
        if (state == null) state = new GamificationState();
        String todayStr = today.toString();
        if (!todayStr.equals(state.getChallengeDate())) {
            state.setChallengeDate(todayStr);
            state.setChallengeProgress(0);
            state.setChallengeCompleted(false);
        }
        return state;
    }

    private void updateStreak(GamificationState state, LocalDate today) {
        String last = state.getLastActivityDate();
        if (today.toString().equals(last)) {
            return;
        }
        if (last != null && !last.isEmpty()) {
            try {
                long days = ChronoUnit.DAYS.between(LocalDate.parse(last), today);
                state.setCurrentStreak(days == 1 ? Math.max(0, state.getCurrentStreak()) + 1 : 1);
            } catch (RuntimeException e) {
                state.setCurrentStreak(1);
            }
        } else {
            state.setCurrentStreak(1);
        }
        state.setLastActivityDate(today.toString());
    }

    private GamificationState loadSync(Context context) {
        File file = new File(context.getFilesDir(), STATE_FILE);
        if (!file.exists()) return new GamificationState();
        try (FileReader reader = new FileReader(file)) {
            GamificationState state = gson.fromJson(reader, GamificationState.class);
            return state != null ? state : new GamificationState();
        } catch (IOException e) {
            Log.w(TAG, "loadSync IOException", e);
            return new GamificationState();
        } catch (com.google.gson.JsonSyntaxException e) {
            File corrupt = new File(context.getFilesDir(), STATE_FILE + ".corrupt." + System.currentTimeMillis());
            boolean renamed = file.renameTo(corrupt);
            Log.w(TAG, "Corrupt gamification state renamed=" + renamed, e);
            return new GamificationState();
        }
    }

    private boolean saveAtomicSync(Context context, GamificationState state) {
        File tmp = new File(context.getFilesDir(), STATE_FILE_TMP);
        File target = new File(context.getFilesDir(), STATE_FILE);
        try (FileOutputStream fos = new FileOutputStream(tmp);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            gson.toJson(state, writer);
            writer.flush();
            fos.getFD().sync();
        } catch (IOException e) {
            Log.e(TAG, "saveAtomicSync write failed", e);
            return false;
        }
        // ponytail: atomic replace target file on minSdk 28+. Fallback to renameTo if filesystem move fails.
        try {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            Log.w(TAG, "saveAtomicSync Files.move failed, falling back to renameTo", e);
        }
        boolean moved = tmp.renameTo(target);
        if (!moved) Log.e(TAG, "saveAtomicSync rename failed");
        return moved;
    }

    private void postOnMain(Context context, Runnable action) {
        if (mainHandler != null) {
            mainHandler.post(action);
        } else {
            action.run();
        }
    }
}
