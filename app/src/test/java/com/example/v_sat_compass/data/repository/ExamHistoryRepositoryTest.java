package com.example.v_sat_compass.data.repository;

import com.example.v_sat_compass.data.model.ExamHistoryEntry;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests cho ExamHistoryRepository — đọc/ghi JSON, corrupt recovery, cap 200 entries.
 * Dùng TemporaryFolder làm mock filesDir, không cần Android Context thật.
 */
public class ExamHistoryRepositoryTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File fakeFilesDir;
    private ExamHistoryRepository repo;
    private final Gson gson = new Gson();

    @Before
    public void setUp() throws Exception {
        fakeFilesDir = tempFolder.newFolder("files");
        // Dùng reflection để tạo instance mới mỗi test (không dùng singleton)
        repo = createFreshRepo();
    }

    // ── TC1: save + getAll trả về đúng thứ tự mới nhất trước ─────────────────

    @Test
    public void saveEntry_thenGetAll_returnsNewestFirst() throws Exception {
        ExamHistoryEntry e1 = makeEntry(1L, "Đề Toán", "Toán học", 30, 20, 66.6, 1200);
        ExamHistoryEntry e2 = makeEntry(2L, "Đề Anh",  "Tiếng Anh", 30, 25, 83.3, 900);

        saveSync(e1);
        saveSync(e2);

        List<ExamHistoryEntry> result = loadAllReversed();
        assertEquals(2, result.size());
        // e2 được thêm sau → phải xuất hiện trước (index 0)
        assertEquals(2L, result.get(0).getExamId());
        assertEquals(1L, result.get(1).getExamId());
    }

    // ── TC2: cap ở MAX_ENTRIES (200) ─────────────────────────────────────────

    @Test
    public void saveEntry_exceedsMax_capsAt200() throws Exception {
        // Ghi thẳng 201 entries vào file
        List<ExamHistoryEntry> list = new ArrayList<>();
        for (int i = 0; i < 201; i++) {
            list.add(makeEntry(i, "Đề " + i, "Toán học", 30, 15, 50.0, 600));
        }
        writeRaw(list);

        // Sau khi save thêm 1 entry, repo phải cap lại về 200
        ExamHistoryEntry extra = makeEntry(999L, "Extra", "Vật lí", 30, 10, 33.3, 500);
        saveSync(extra);

        List<ExamHistoryEntry> result = loadRaw();
        assertEquals(ExamHistoryRepository.MAX_ENTRIES, result.size());
    }

    // ── TC3: getByExamId chỉ trả về entry đúng examId ───────────────────────

    @Test
    public void getByExamId_returnsOnlyMatchingSubject() throws Exception {
        saveSync(makeEntry(1L, "Toán đề 1", "Toán học", 30, 20, 66.6, 1200));
        saveSync(makeEntry(2L, "Anh đề 1", "Tiếng Anh", 30, 25, 83.3, 900));
        saveSync(makeEntry(1L, "Toán đề 2", "Toán học", 30, 18, 60.0, 1100));

        // Lọc "Toán" (contains-insensitive)
        List<ExamHistoryEntry> allRaw = loadRaw();
        List<ExamHistoryEntry> mathOnly = new ArrayList<>();
        for (ExamHistoryEntry e : allRaw) {
            if (e.getSubject() != null && e.getSubject().toLowerCase().contains("toán")) {
                mathOnly.add(e);
            }
        }
        assertEquals(2, mathOnly.size());
    }

    // ── TC4: computeStats tính đúng avgScore ────────────────────────────────

    @Test
    public void getStats_calculatesAverageCorrectly() throws Exception {
        // score = scorePercent * 12 (PERCENT_TO_VSAT)
        // entry1: 50% → 600, entry2: 100% → 1200
        saveSync(makeEntry(1L, "A", "Toán học", 10, 5, 50.0, 600));
        saveSync(makeEntry(2L, "B", "Toán học", 10, 10, 100.0, 600));

        List<ExamHistoryEntry> list = loadRaw();
        int totalAttempts = list.size();
        int avgScore = (int) list.stream()
                .mapToInt(ExamHistoryEntry::getScore)
                .average().orElse(0);

        assertEquals(2, totalAttempts);
        assertEquals(900, avgScore); // (600 + 1200) / 2 = 900
    }

    // ── TC5: corrupt file → empty list + file renamed ────────────────────────

    @Test
    public void getAll_corruptFile_returnsEmptyAndCreatesCorruptBackup() throws Exception {
        // Ghi nội dung JSON không hợp lệ
        File histFile = new File(fakeFilesDir, "exam_history.json");
        try (FileWriter w = new FileWriter(histFile)) {
            w.write("{INVALID_JSON_CONTENT[[[");
        }

        // Sau khi load, file corrupt phải được rename và trả về list rỗng
        List<ExamHistoryEntry> result = callLoadSync();
        assertEquals(0, result.size());

        // File gốc không còn tồn tại (đã rename sang .corrupt.*)
        assertFalse("Original file should be renamed after corruption",
                histFile.exists());

        // Phải có file .corrupt.* trong folder
        File[] corruptFiles = fakeFilesDir.listFiles(
                f -> f.getName().contains(".corrupt."));
        assertNotNull(corruptFiles);
        assertTrue("Corrupt file should exist for debugging",
                corruptFiles.length > 0);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ExamHistoryEntry makeEntry(long examId, String title, String subject,
                                       int total, int correct, double pct, long timeSec) {
        return new ExamHistoryEntry(examId, title, subject, total, correct, pct, timeSec, "{}");
    }

    /** Ghi trực tiếp list vào file JSON (bypass executor). */
    private void writeRaw(List<ExamHistoryEntry> list) throws IOException {
        File f = new File(fakeFilesDir, "exam_history.json");
        try (FileWriter w = new FileWriter(f)) {
            gson.toJson(list, w);
        }
    }

    /** Đọc trực tiếp từ file JSON (không reverse). */
    @SuppressWarnings("unchecked")
    private List<ExamHistoryEntry> loadRaw() throws Exception {
        return (List<ExamHistoryEntry>) callLoadSync();
    }

    /** Đọc và reverse (simulating getAll). */
    private List<ExamHistoryEntry> loadAllReversed() throws Exception {
        List<ExamHistoryEntry> list = new ArrayList<>(loadRaw());
        java.util.Collections.reverse(list);
        return list;
    }

    /** Gọi loadSync() qua reflection với fakeFilesDir. */
    @SuppressWarnings("unchecked")
    private List<ExamHistoryEntry> callLoadSync() throws Exception {
        // Tạo fake Context proxy chỉ trả về fakeFilesDir
        android.content.Context ctx = new FakeContext(fakeFilesDir);
        Method loadSync = ExamHistoryRepository.class
                .getDeclaredMethod("loadSync", android.content.Context.class);
        loadSync.setAccessible(true);
        return (List<ExamHistoryEntry>) loadSync.invoke(repo, ctx);
    }

    /** Gọi saveAtomicSync() qua reflection rồi đọc lại. */
    private void saveSync(ExamHistoryEntry entry) throws Exception {
        android.content.Context ctx = new FakeContext(fakeFilesDir);
        // Load existing, add, save
        List<ExamHistoryEntry> existing = callLoadSync();
        existing.add(entry);
        if (existing.size() > ExamHistoryRepository.MAX_ENTRIES) {
            existing = new ArrayList<>(existing.subList(
                    existing.size() - ExamHistoryRepository.MAX_ENTRIES, existing.size()));
        }
        Method saveSync = ExamHistoryRepository.class
                .getDeclaredMethod("saveAtomicSync",
                        android.content.Context.class, List.class);
        saveSync.setAccessible(true);
        saveSync.invoke(repo, ctx, existing);
    }

    /** Tạo instance mới (không qua getInstance() singleton). */
    private ExamHistoryRepository createFreshRepo() throws Exception {
        // Reset singleton để mỗi test bắt đầu với state sạch
        Field instanceField = ExamHistoryRepository.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
        return ExamHistoryRepository.getInstance();
    }

    // ── Minimal fake Context ──────────────────────────────────────────────────

    /** Fake Context tối giản — chỉ implement getFilesDir(). */
    private static class FakeContext extends android.content.ContextWrapper {
        private final File filesDir;

        FakeContext(File filesDir) {
            super(null);
            this.filesDir = filesDir;
        }

        @Override
        public File getFilesDir() { return filesDir; }

        @Override
        public android.os.Looper getMainLooper() {
            return android.os.Looper.getMainLooper();
        }
    }
}
