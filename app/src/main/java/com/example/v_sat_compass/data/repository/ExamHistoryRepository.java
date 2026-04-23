package com.example.v_sat_compass.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.v_sat_compass.data.model.ExamHistoryEntry;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lưu lịch sử bài làm vào file JSON trong internal storage.
 * Mọi I/O đều chạy trên background thread để không block UI.
 * Atomic write (tmp → rename) bảo vệ khỏi half-written nếu app bị kill giữa chừng.
 */
public class ExamHistoryRepository {

    private static final String TAG = "ExamHistoryRepository";

    private static final String HISTORY_FILE = "exam_history.json";
    private static final String HISTORY_FILE_TMP = "exam_history.json.tmp";

    /** Số entry tối đa giữ lại — cũ hơn sẽ bị xóa. */
    public static final int MAX_ENTRIES = 200;

    private static ExamHistoryRepository instance;

    // Single-thread executor đảm bảo read/write tuần tự, không race condition
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Gson gson = new Gson();

    // Lock object cho synchronized block bảo vệ file I/O
    private final Object fileLock = new Object();

    private ExamHistoryRepository() {}

    public static synchronized ExamHistoryRepository getInstance() {
        if (instance == null) {
            instance = new ExamHistoryRepository();
        }
        return instance;
    }

    public interface Callback<T> {
        void onResult(T result);
    }

    /** Lưu một entry vào lịch sử (async, fire-and-forget). */
    public void saveEntry(Context context, ExamHistoryEntry entry) {
        saveEntry(context, entry, null);
    }

    /**
     * Lưu một entry vào lịch sử (async). Gọi {@code onSaveFailed} trên main thread nếu thất bại.
     * @param onSaveFailed Runnable chạy trên main thread khi lưu thất bại (null = ignore)
     */
    public void saveEntry(Context context, ExamHistoryEntry entry, Runnable onSaveFailed) {
        final Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            boolean saved;
            synchronized (fileLock) {
                List<ExamHistoryEntry> list = loadSync(appCtx);
                list.add(entry);
                if (list.size() > MAX_ENTRIES) {
                    list = new ArrayList<>(list.subList(list.size() - MAX_ENTRIES, list.size()));
                }
                saved = saveAtomicSync(appCtx, list);
            }
            if (!saved) {
                Log.e(TAG, "saveEntry() failed — history entry may not be persisted");
                if (onSaveFailed != null) {
                    postOnMain(appCtx, onSaveFailed);
                }
            }
        });
    }

    /** DEBUG ONLY — inject N entries giả để test stress/empty/scroll. */
    public void injectMockEntries(Context context, int count, Runnable onDone) {
        final Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            synchronized (fileLock) {
                List<ExamHistoryEntry> list = loadSync(appCtx);
                String[] subjects = {"Toán học", "Tiếng Anh", "Vật lí"};
                String[] titles = {"Đề thi thử Toán", "Đề thi Tiếng Anh", "Đề thi Vật lí"};
                long baseId = System.currentTimeMillis();
                for (int i = 0; i < count; i++) {
                    int subjectIdx = i % subjects.length;
                    ExamHistoryEntry e = new ExamHistoryEntry(
                            (long) (subjectIdx + 1),
                            titles[subjectIdx] + " #" + (i + 1),
                            subjects[subjectIdx],
                            30, 15 + (i % 15),
                            50.0 + (i % 40),
                            1800 + (i * 60L),
                            "{}");
                    // Space entries 1 minute apart for realistic timestamps
                    list.add(e);
                }
                if (list.size() > MAX_ENTRIES) {
                    list = new ArrayList<>(list.subList(list.size() - MAX_ENTRIES, list.size()));
                }
                saveAtomicSync(appCtx, list);
            }
            if (onDone != null) postOnMain(appCtx, onDone);
        });
    }

    /** Lấy tất cả lịch sử (async, callback trên main thread). */
    public void getAll(Context context, Callback<List<ExamHistoryEntry>> callback) {
        final Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            List<ExamHistoryEntry> list;
            synchronized (fileLock) {
                list = loadSync(appCtx);
            }
            Collections.reverse(list);
            postOnMain(appCtx, () -> callback.onResult(list));
        });
    }

    /** Lấy N entry gần nhất (async). */
    public void getRecent(Context context, int limit, Callback<List<ExamHistoryEntry>> callback) {
        final Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            List<ExamHistoryEntry> all;
            synchronized (fileLock) {
                all = loadSync(appCtx);
            }
            Collections.reverse(all);
            List<ExamHistoryEntry> recent = new ArrayList<>(
                    all.subList(0, Math.min(limit, all.size())));
            postOnMain(appCtx, () -> callback.onResult(recent));
        });
    }

    /** Lấy entries theo môn (async). Truyền null hoặc rỗng để lấy tất cả. */
    public void getBySubject(Context context, String subject, Callback<List<ExamHistoryEntry>> callback) {
        final Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            List<ExamHistoryEntry> all;
            synchronized (fileLock) {
                all = loadSync(appCtx);
            }
            Collections.reverse(all);
            List<ExamHistoryEntry> filtered = new ArrayList<>();
            for (ExamHistoryEntry e : all) {
                if (subject == null || subject.isEmpty()
                        || (e.getSubject() != null
                        && e.getSubject().toLowerCase().contains(subject.toLowerCase()))) {
                    filtered.add(e);
                }
            }
            postOnMain(appCtx, () -> callback.onResult(filtered));
        });
    }

    /** Tính thống kê tổng hợp (async). */
    public void getStats(Context context, Callback<HistoryStats> callback) {
        final Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            List<ExamHistoryEntry> all;
            synchronized (fileLock) {
                all = loadSync(appCtx);
            }
            HistoryStats stats = computeStats(all);
            postOnMain(appCtx, () -> callback.onResult(stats));
        });
    }

    /** Lấy điểm cao nhất theo examId (async). Trả -1 nếu không có entry nào. */
    public void getBestScoreForExam(Context context, long examId, Callback<Integer> callback) {
        final Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            List<ExamHistoryEntry> all;
            synchronized (fileLock) {
                all = loadSync(appCtx);
            }
            int best = -1;
            for (ExamHistoryEntry e : all) {
                if (e.getExamId() == examId && e.getScore() > best) {
                    best = e.getScore();
                }
            }
            final int finalBest = best;
            postOnMain(appCtx, () -> callback.onResult(finalBest));
        });
    }

    /** DEBUG ONLY — xóa toàn bộ lịch sử. */
    public void clearAll(Context context, Runnable onDone) {
        final Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            synchronized (fileLock) {
                saveAtomicSync(appCtx, new ArrayList<>());
            }
            if (onDone != null) postOnMain(appCtx, onDone);
        });
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private HistoryStats computeStats(List<ExamHistoryEntry> list) {
        if (list.isEmpty()) return new HistoryStats(0, 0, 0, 0);
        int totalCorrect = 0;
        long totalTimeSeconds = 0;
        for (ExamHistoryEntry e : list) {
            totalCorrect += e.getCorrectCount();
            totalTimeSeconds += e.getTimeSpentSeconds();
        }
        int avgScore = (int) list.stream().mapToInt(ExamHistoryEntry::getScore).average().orElse(0);
        return new HistoryStats(list.size(), totalCorrect, totalTimeSeconds, avgScore);
    }

    /**
     * Đọc file lịch sử từ disk. Nếu file bị corrupt, rename sang .corrupt.<ts> và trả về empty list.
     * Phải gọi trong synchronized(fileLock) hoặc từ executor thread.
     */
    private List<ExamHistoryEntry> loadSync(Context context) {
        File file = new File(context.getFilesDir(), HISTORY_FILE);
        if (!file.exists()) return new ArrayList<>();
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<List<ExamHistoryEntry>>() {}.getType();
            List<ExamHistoryEntry> list = gson.fromJson(reader, type);
            return list != null ? list : new ArrayList<>();
        } catch (IOException e) {
            Log.w(TAG, "loadSync() IOException reading history file", e);
            return new ArrayList<>();
        } catch (com.google.gson.JsonSyntaxException e) {
            // File bị corrupt — rename để preserve debug info, tạo lại empty
            File corrupt = new File(context.getFilesDir(),
                    HISTORY_FILE + ".corrupt." + System.currentTimeMillis());
            boolean renamed = file.renameTo(corrupt);
            Log.w(TAG, "loadSync() JSON corrupt, renamed to " + corrupt.getName()
                    + " (renamed=" + renamed + ")");
            return new ArrayList<>();
        }
    }

    /**
     * Ghi atomic: write vào .tmp trước, flush, rồi rename sang file thật.
     * Bảo vệ khỏi half-written nếu app bị kill giữa chừng.
     * @return true nếu thành công
     */
    private boolean saveAtomicSync(Context context, List<ExamHistoryEntry> list) {
        File tmpFile = new File(context.getFilesDir(), HISTORY_FILE_TMP);
        File targetFile = new File(context.getFilesDir(), HISTORY_FILE);
        try (FileOutputStream fos = new FileOutputStream(tmpFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            gson.toJson(list, writer);
            writer.flush();
            fos.getFD().sync(); // đảm bảo flush xuống disk trước khi rename
        } catch (IOException e) {
            Log.e(TAG, "saveAtomicSync() failed writing tmp file", e);
            return false;
        }
        // Atomic rename trên Android API 26+ (minSdk=28, OK)
        boolean moved = tmpFile.renameTo(targetFile);
        if (!moved) {
            Log.e(TAG, "saveAtomicSync() renameTo failed — disk may be full");
        }
        return moved;
    }

    private void postOnMain(Context context, Runnable action) {
        new android.os.Handler(context.getMainLooper()).post(action);
    }

    // ── Stats DTO ─────────────────────────────────────────────────────────────

    /** Thống kê tổng hợp trả về từ getStats(). */
    public static class HistoryStats {
        public final int totalAttempts;
        public final int totalCorrect;
        public final long totalTimeSeconds;
        public final int avgScore;

        public HistoryStats(int totalAttempts, int totalCorrect,
                            long totalTimeSeconds, int avgScore) {
            this.totalAttempts = totalAttempts;
            this.totalCorrect = totalCorrect;
            this.totalTimeSeconds = totalTimeSeconds;
            this.avgScore = avgScore;
        }
    }
}
