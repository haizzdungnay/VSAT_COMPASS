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
            if (finalFile.exists()) {
                finalFile.delete();
            }
            if (!tempFile.renameTo(finalFile)) {
                writeEncryptedFile(context, finalFile, bytes);
                tempFile.delete();
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

    public synchronized OfflineExamBundle loadBundle(Context context, long examId) {
        File file = bundleFile(context, examId);
        if (!file.exists()) {
            return null;
        }
        try {
            byte[] bytes = readEncryptedFile(context, file);
            String json = new String(bytes, StandardCharsets.UTF_8);
            return gson.fromJson(json, OfflineExamBundle.class);
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

    private void writeEncryptedFile(Context context, File file, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] masked = new byte[data.length];
            for (int i = 0; i < data.length; i++) {
                masked[i] = (byte) (data[i] ^ 0x5A);
            }
            fos.write(masked);
            fos.flush();
        }
    }

    private byte[] readEncryptedFile(Context context, File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int read;
            while ((read = fis.read(buf)) != -1) {
                bos.write(buf, 0, read);
            }
            byte[] data = bos.toByteArray();
            byte[] unmasked = new byte[data.length];
            for (int i = 0; i < data.length; i++) {
                unmasked[i] = (byte) (data[i] ^ 0x5A);
            }
            return unmasked;
        }
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
