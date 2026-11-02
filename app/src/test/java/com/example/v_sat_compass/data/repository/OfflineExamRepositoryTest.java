package com.example.v_sat_compass.data.repository;

import android.content.Context;
import android.content.ContextWrapper;

import com.example.v_sat_compass.data.model.OfflineExamBundle;
import com.example.v_sat_compass.data.model.Question;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

public class OfflineExamRepositoryTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File fakeFilesDir;
    private Context fakeContext;
    private OfflineExamRepository repository;
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
        repository = new OfflineExamRepository(Executors.newSingleThreadExecutor(), null, gson);
    }

    private OfflineExamBundle createMockBundle(long examId, String title) {
        String json = "{\n" +
                "  \"id\": " + examId + ",\n" +
                "  \"examCode\": \"VSAT_MATH_2026\",\n" +
                "  \"title\": \"" + title + "\",\n" +
                "  \"questionCount\": 2,\n" +
                "  \"durationMinutes\": 60,\n" +
                "  \"questions\": [\n" +
                "    {\n" +
                "      \"id\": 1001,\n" +
                "      \"questionCode\": \"MATH_01\",\n" +
                "      \"questionText\": \"Gia tri cua x la bao nhieu?\",\n" +
                "      \"questionType\": \"SINGLE_CHOICE\",\n" +
                "      \"explanation\": \"Loi giai chi tiet\",\n" +
                "      \"options\": [\n" +
                "        {\n" +
                "          \"id\": 5001,\n" +
                "          \"optionLabel\": \"A\",\n" +
                "          \"optionText\": \"10\",\n" +
                "          \"isCorrect\": true,\n" +
                "          \"displayOrder\": 1\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        return gson.fromJson(json, OfflineExamBundle.class);
    }

    @Test
    public void saveBundle_encryptsWithAesGcm_andLoadsCorrectly() throws Exception {
        long examId = 101L;
        OfflineExamBundle bundle = createMockBundle(examId, "De thi thu V-SAT 2026");

        // 1. Save bundle
        repository.saveBundle(fakeContext, examId, bundle);

        // 2. Check file exists and has AES-GCM header
        File file = repository.bundleFile(fakeContext, examId);
        assertTrue("Bundle file must exist", file.exists());

        byte[] rawBytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            int read = fis.read(rawBytes);
            assertEquals(file.length(), read);
        }

        // Header check: 'V', 'S', 'A', 'T', 0x02
        assertEquals('V', (char) rawBytes[0]);
        assertEquals('S', (char) rawBytes[1]);
        assertEquals('A', (char) rawBytes[2]);
        assertEquals('T', (char) rawBytes[3]);
        assertEquals(0x02, rawBytes[4]);

        // Verify content is encrypted (does not contain plaintext question)
        String rawString = new String(rawBytes, StandardCharsets.UTF_8);
        assertFalse("Ciphertext must not contain plaintext string", rawString.contains("MATH_01"));

        // 3. Load bundle
        OfflineExamBundle loaded = repository.loadBundle(fakeContext, examId);
        assertNotNull("Loaded bundle must not be null", loaded);
        assertEquals(examId, (long) loaded.getId());
        assertEquals("De thi thu V-SAT 2026", loaded.getTitle());
        assertEquals(2, (int) loaded.getQuestionCount());
        assertEquals(60, (int) loaded.getDurationMinutes());

        // 4. Load question and question ids
        List<Long> ids = repository.loadQuestionIds(fakeContext, examId);
        assertEquals(1, ids.size());
        assertEquals(1001L, (long) ids.get(0));

        Question question = repository.loadQuestion(fakeContext, examId, 1001L);
        assertNotNull(question);
        assertEquals("MATH_01", question.getQuestionCode());
        assertEquals("Gia tri cua x la bao nhieu?", question.getQuestionText());
        assertEquals("Loi giai chi tiet", question.getExplanation());
        assertNotNull(question.getOptions());
        assertEquals(1, question.getOptions().size());
        assertTrue(question.getOptions().get(0).isCorrect());
    }

    @Test
    public void loadBundle_legacyXorMaskedFile_loadsSuccessfully() throws Exception {
        long examId = 202L;
        OfflineExamBundle bundle = createMockBundle(examId, "Legacy XOR De Thi");

        String json = gson.toJson(bundle);
        byte[] rawJsonBytes = json.getBytes(StandardCharsets.UTF_8);
        byte[] xorBytes = new byte[rawJsonBytes.length];
        for (int i = 0; i < rawJsonBytes.length; i++) {
            xorBytes[i] = (byte) (rawJsonBytes[i] ^ 0x5A);
        }

        File dir = new File(fakeFilesDir, "offline_exams");
        assertTrue(dir.mkdirs() || dir.exists());
        File legacyFile = new File(dir, examId + ".vsat");
        try (FileOutputStream fos = new FileOutputStream(legacyFile)) {
            fos.write(xorBytes);
        }

        // Load via repository
        OfflineExamBundle loaded = repository.loadBundle(fakeContext, examId);
        assertNotNull("Legacy XOR bundle must load successfully", loaded);
        assertEquals(examId, (long) loaded.getId());
        assertEquals("Legacy XOR De Thi", loaded.getTitle());
        assertEquals(60, (int) loaded.getDurationMinutes());

        // Verify auto-migration to AES-GCM on disk
        byte[] migratedBytes = new byte[(int) legacyFile.length()];
        try (FileInputStream fis = new FileInputStream(legacyFile)) {
            fis.read(migratedBytes);
        }
        assertEquals('V', (char) migratedBytes[0]);
        assertEquals('S', (char) migratedBytes[1]);
        assertEquals('A', (char) migratedBytes[2]);
        assertEquals('T', (char) migratedBytes[3]);
        assertEquals(0x02, migratedBytes[4]);
    }

    @Test
    public void isDownloaded_and_deleteExam_workAsExpected() throws Exception {
        long examId = 303L;
        OfflineExamBundle bundle = createMockBundle(examId, "De Thi Xoa");

        assertFalse(repository.isDownloaded(fakeContext, examId));

        repository.saveBundle(fakeContext, examId, bundle);
        assertTrue(repository.isDownloaded(fakeContext, examId));

        boolean deleted = repository.deleteExam(fakeContext, examId);
        assertTrue(deleted);
        assertFalse(repository.isDownloaded(fakeContext, examId));
    }

    @Test
    public void loadBundle_tamperedCiphertext_returnsNullSafely() throws Exception {
        long examId = 404L;
        OfflineExamBundle bundle = createMockBundle(examId, "Tamper Test");
        repository.saveBundle(fakeContext, examId, bundle);

        File file = repository.bundleFile(fakeContext, examId);
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(bytes);
        }

        // Tamper 1 byte in the ciphertext portion (after 5-byte magic + 12-byte IV)
        bytes[20] ^= (byte) 0xFF;
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
        }

        OfflineExamBundle loaded = repository.loadBundle(fakeContext, examId);
        assertNull("Tampered bundle must fail GCM auth and return null", loaded);
    }

    @Test
    public void loadBundle_shortOrCorruptFile_returnsNullSafely() throws Exception {
        long examId = 505L;
        File dir = new File(fakeFilesDir, "offline_exams");
        assertTrue(dir.mkdirs() || dir.exists());
        File shortFile = new File(dir, examId + ".vsat");
        try (FileOutputStream fos = new FileOutputStream(shortFile)) {
            fos.write(new byte[]{1, 2, 3, 4, 5});
        }

        OfflineExamBundle loaded = repository.loadBundle(fakeContext, examId);
        assertNull("Short/corrupted file must return null without crashing", loaded);
    }

    @Test
    public void loadBundle_truncatedHeader_returnsNullSafely() throws Exception {
        long examId = 606L;
        File dir = new File(fakeFilesDir, "offline_exams");
        assertTrue(dir.mkdirs() || dir.exists());
        File truncFile = new File(dir, examId + ".vsat");
        try (FileOutputStream fos = new FileOutputStream(truncFile)) {
            fos.write(new byte[]{'V', 'S', 'A', 'T', 0x02, 1, 2, 3});
        }

        OfflineExamBundle loaded = repository.loadBundle(fakeContext, examId);
        assertNull("Truncated AES-GCM bundle must throw IOException and return null safely", loaded);
    }
}
