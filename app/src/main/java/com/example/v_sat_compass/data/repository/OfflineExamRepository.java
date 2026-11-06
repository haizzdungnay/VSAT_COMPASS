package com.example.v_sat_compass.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.OfflineExamBundle;
import com.example.v_sat_compass.data.model.Question;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for offline encrypted exam bundles.
 */
public class OfflineExamRepository {

    private static final String TAG = "OfflineExamRepo";
    private static final String DIR_NAME = "offline_exams";
    private static final String FILE_EXTENSION = ".vsat";

    private static final byte[] MAGIC_HEADER = new byte[]{'V', 'S', 'A', 'T', 0x02};
    // ponytail: App-level AES-128 obfuscation per PLAN_offline_phase_f.md (2026-06-18). Prevents casual inspection of answers in local files. Upgrade to Android Keystore when commercial DRM is required.
    private static final byte[] AES_KEY_BYTES = new byte[]{
            0x56, 0x53, 0x41, 0x54, 0x5f, 0x43, 0x4f, 0x4d, 0x50, 0x41, 0x53, 0x53, 0x5f, 0x4b, 0x45, 0x59
    };

    private static volatile OfflineExamRepository instance;

    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Gson gson;

    OfflineExamRepository(ExecutorService executor, Handler mainHandler, Gson gson) {
        this.executor = executor;
        this.mainHandler = mainHandler;
        this.gson = gson;
    }

    public static OfflineExamRepository getInstance() {
        if (instance == null) {
            synchronized (OfflineExamRepository.class) {
                if (instance == null) {
                    instance = new OfflineExamRepository(
                            Executors.newSingleThreadExecutor(),
                            new Handler(Looper.getMainLooper()),
                            new Gson()
                    );
                }
            }
        }
        return instance;
    }

    public interface DownloadCallback {
        void onSuccess(OfflineExamBundle bundle);
        void onError(String message);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface ListDownloadedCallback {
        void onLoaded(List<DownloadedExam> exams, long totalBytes);
    }

    public interface QuestionIdsCallback {
        void onResult(List<Long> ids);
    }

    public interface QuestionCallback {
        void onResult(Question question);
    }

    public void downloadExam(Context context, long examId, DownloadCallback callback) {
        Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            try {
                com.example.v_sat_compass.data.api.ExamApi api =
                        com.example.v_sat_compass.data.api.ApiClient.getClient()
                                .create(com.example.v_sat_compass.data.api.ExamApi.class);

                retrofit2.Response<ApiResponse<OfflineExamBundle>> response =
                        api.downloadExamBundle(examId).execute();

                if (!response.isSuccessful() || response.body() == null || response.body().getData() == null) {
                    String err = "Loi tai de thi (HTTP " + response.code() + ")";
                    mainHandler.post(() -> callback.onError(err));
                    return;
                }

                OfflineExamBundle bundle = response.body().getData();
                saveBundle(appCtx, examId, bundle);
                mainHandler.post(() -> callback.onSuccess(bundle));

            } catch (Exception e) {
                Log.e(TAG, "downloadExam failed for id " + examId, e);
                mainHandler.post(() -> callback.onError("Loi ket noi: " + e.getMessage()));
            }
        });
    }

    public synchronized void saveBundle(Context context, long examId, OfflineExamBundle bundle)
            throws IOException {
        File dir = examDir(context);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Cannot create directory " + dir.getAbsolutePath());
        }

        String json = gson.toJson(bundle);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        File finalFile = bundleFile(context, examId);
        File tempFile = new File(dir, examId + ".tmp");

        if (tempFile.exists()) {
            tempFile.delete();
        }

        try {
            writeEncryptedFile(context, tempFile, bytes);
            try {
                java.nio.file.Files.move(tempFile.toPath(), finalFile.toPath(),
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                java.nio.file.Files.move(tempFile.toPath(), finalFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            if (tempFile.exists()) {
                tempFile.delete();
            }
            throw e;
        }
    }

    public Question loadQuestion(Context context, long examId, long questionId) {
        OfflineExamBundle bundle = loadBundle(context, examId);
        if (bundle == null || bundle.getQuestions() == null) {
            return null;
        }
        for (OfflineExamBundle.QuestionBundleItem question : bundle.getQuestions()) {
            if (question.getId() != null && question.getId() == questionId) {
                return toQuestion(question);
            }
        }
        return null;
    }

    public List<Long> loadQuestionIds(Context context, long examId) {
        OfflineExamBundle bundle = loadBundle(context, examId);
        if (bundle == null || bundle.getQuestions() == null) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (OfflineExamBundle.QuestionBundleItem question : bundle.getQuestions()) {
            if (question.getId() != null) {
                ids.add(question.getId());
            }
        }
        return ids;
    }

    public void loadQuestionIds(Context context, long examId, QuestionIdsCallback callback) {
        Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            List<Long> ids = loadQuestionIds(appCtx, examId);
            mainHandler.post(() -> callback.onResult(ids));
        });
    }

    public void loadQuestion(Context context, long examId, long questionId, QuestionCallback callback) {
        Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            Question question = loadQuestion(appCtx, examId, questionId);
            mainHandler.post(() -> callback.onResult(question));
        });
    }

    public void findDownloadedExamIds(Context context, List<Long> examIds, DownloadedIdsCallback callback) {
        Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            List<Long> downloaded = new ArrayList<>();
            for (Long id : examIds) {
                if (id != null && isDownloaded(appCtx, id)) {
                    downloaded.add(id);
                }
            }
            mainHandler.post(() -> callback.onResult(downloaded));
        });
    }

    public interface DownloadedIdsCallback {
        void onResult(List<Long> downloadedIds);
    }

    private static class DecryptedResult {
        final byte[] bytes;
        final boolean isLegacy;

        DecryptedResult(byte[] bytes, boolean isLegacy) {
            this.bytes = bytes;
            this.isLegacy = isLegacy;
        }
    }

    public synchronized OfflineExamBundle loadBundle(Context context, long examId) {
        File file = bundleFile(context, examId);
        if (!file.exists()) {
            return null;
        }
        try {
            DecryptedResult result = readEncryptedFile(context, file);
            String json = new String(result.bytes, StandardCharsets.UTF_8);
            OfflineExamBundle bundle = gson.fromJson(json, OfflineExamBundle.class);
            // Auto-migrate legacy XOR files to AES-GCM on read without reading disk twice
            if (bundle != null && result.isLegacy) {
                try {
                    saveBundle(context, examId, bundle);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to auto-migrate legacy bundle " + examId + " to AES-GCM", e);
                }
            }
            return bundle;
        } catch (Exception e) {
            Log.e(TAG, "loadBundle failed for id " + examId, e);
            return null;
        }
    }

    public synchronized boolean isDownloaded(Context context, long examId) {
        return bundleFile(context, examId).exists();
    }

    public synchronized boolean deleteExam(Context context, long examId) {
        File file = bundleFile(context, examId);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    public void delete(Context context, long examId, Runnable callback) {
        Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            deleteExam(appCtx, examId);
            if (callback != null) {
                mainHandler.post(callback::run);
            }
        });
    }

    public void listDownloaded(Context context, ListDownloadedCallback callback) {
        Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            List<DownloadedExam> result = new ArrayList<>();
            long totalBytes = 0;
            File dir = examDir(appCtx);
            if (dir.exists()) {
                File[] files = dir.listFiles((d, name) -> name.endsWith(FILE_EXTENSION));
                if (files != null) {
                    for (File file : files) {
                        try {
                            String nameWithoutExt = file.getName().replace(FILE_EXTENSION, "");
                            long examId = Long.parseLong(nameWithoutExt);
                            OfflineExamBundle bundle = loadBundle(appCtx, examId);
                            if (bundle != null) {
                                totalBytes += file.length();
                                result.add(new DownloadedExam(
                                        examId,
                                        bundle.getTitle() != null ? bundle.getTitle() : "De thi #" + examId,
                                        bundle.getQuestionCount() != null ? bundle.getQuestionCount() : 0,
                                        bundle.getDurationMinutes() != null ? bundle.getDurationMinutes() : 0,
                                        file.length()));
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
            final long finalBytes = totalBytes;
            mainHandler.post(() -> callback.onLoaded(result, finalBytes));
        });
    }

    public static byte[] encryptMediaBytes(byte[] data) throws IOException, GeneralSecurityException {
        if (data == null) return new byte[0];
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(AES_KEY_BYTES, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

        byte[] cipherText = cipher.doFinal(data);

        byte[] result = new byte[MAGIC_HEADER.length + iv.length + cipherText.length];
        System.arraycopy(MAGIC_HEADER, 0, result, 0, MAGIC_HEADER.length);
        System.arraycopy(iv, 0, result, MAGIC_HEADER.length, iv.length);
        System.arraycopy(cipherText, 0, result, MAGIC_HEADER.length + iv.length, cipherText.length);
        return result;
    }

    public static byte[] decryptMediaBytes(byte[] encryptedBytes) throws IOException, GeneralSecurityException {
        if (encryptedBytes == null || encryptedBytes.length == 0) return new byte[0];

        if (!startsWith(encryptedBytes, MAGIC_HEADER)) {
            throw new IOException("Invalid media bundle: missing VSAT\\x02 magic header");
        }
        if (encryptedBytes.length < MAGIC_HEADER.length + 12 + 16) {
            throw new IOException("Corrupted AES-GCM bundle: truncated file (< 33 bytes)");
        }

        byte[] iv = new byte[12];
        System.arraycopy(encryptedBytes, MAGIC_HEADER.length, iv, 0, 12);
        int cipherOffset = MAGIC_HEADER.length + 12;
        int cipherLen = encryptedBytes.length - cipherOffset;

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(AES_KEY_BYTES, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

        return cipher.doFinal(encryptedBytes, cipherOffset, cipherLen);
    }

    public byte[] encryptMedia(byte[] data) throws IOException, GeneralSecurityException {
        return encryptMediaBytes(data);
    }

    public byte[] decryptMedia(byte[] encryptedBytes) throws IOException, GeneralSecurityException {
        return decryptMediaBytes(encryptedBytes);
    }

    private void writeEncryptedFile(Context context, File file, byte[] data) throws IOException {
        try {
            byte[] encrypted = encryptMediaBytes(data);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(encrypted);
                fos.flush();
            }
        } catch (GeneralSecurityException e) {
            throw new IOException("Failed to encrypt bundle with AES-GCM", e);
        }
    }

    private DecryptedResult readEncryptedFile(Context context, File file) throws IOException {
        byte[] fileBytes;
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int read;
            while ((read = fis.read(buf)) != -1) {
                bos.write(buf, 0, read);
            }
            fileBytes = bos.toByteArray();
        }

        if (startsWith(fileBytes, MAGIC_HEADER)) {
            try {
                return new DecryptedResult(decryptMediaBytes(fileBytes), false);
            } catch (GeneralSecurityException e) {
                throw new IOException("Failed to decrypt AES-GCM bundle: auth tag verification failed", e);
            }
        }

        // Fallback for legacy XOR-masked bundles (backward compatibility)
        byte[] unmasked = new byte[fileBytes.length];
        for (int i = 0; i < fileBytes.length; i++) {
            unmasked[i] = (byte) (fileBytes[i] ^ 0x5A);
        }
        return new DecryptedResult(unmasked, true);
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    public File bundleFile(Context context, long examId) {
        return new File(examDir(context), examId + FILE_EXTENSION);
    }

    private Question toQuestion(OfflineExamBundle.QuestionBundleItem source) {
        JsonObject json = new JsonObject();
        json.addProperty("id", source.getId());
        json.addProperty("question_code", source.getQuestionCode());
        json.addProperty("question_text", source.getQuestionText());
        json.addProperty("question_type", source.getQuestionType());
        json.addProperty("explanation", source.getExplanation());
        if (source.getQuestionGroupId() != null) { json.addProperty("question_group_id", source.getQuestionGroupId()); }
        if (source.getQuestionGroup() != null) { json.add("question_group", gson.toJsonTree(source.getQuestionGroup())); }
        JsonArray options = new JsonArray();
        if (source.getOptions() != null) {
            for (OfflineExamBundle.OptionBundleItem option : source.getOptions()) {
                JsonObject opt = new JsonObject();
                opt.addProperty("id", option.getId());
                opt.addProperty("option_label", option.getOptionLabel());
                opt.addProperty("option_text", option.getOptionText());
                opt.addProperty("is_correct", Boolean.TRUE.equals(option.getIsCorrect()));
                opt.addProperty("display_order", option.getDisplayOrder() != null ? option.getDisplayOrder() : 0);
                options.add(opt);
            }
        }
        json.add("options", options);
        return gson.fromJson(json, Question.class);
    }

    private File examDir(Context context) {
        return new File(context.getFilesDir(), DIR_NAME);
    }

    public static class DownloadedExam {
        public final long examId;
        public final String title;
        public final int questionCount;
        public final int durationMinutes;
        public final long sizeBytes;

        public DownloadedExam(long examId, String title, int questionCount, int durationMinutes, long sizeBytes) {
            this.examId = examId;
            this.title = title;
            this.questionCount = questionCount;
            this.durationMinutes = durationMinutes;
            this.sizeBytes = sizeBytes;
        }
    }
}
