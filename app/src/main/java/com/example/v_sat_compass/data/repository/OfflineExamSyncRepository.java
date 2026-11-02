package com.example.v_sat_compass.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.ExamApi;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.ClientSubmitRequest;
import com.example.v_sat_compass.data.model.ExamSession;
import com.example.v_sat_compass.sync.OfflineExamSyncWorker;
import com.example.v_sat_compass.util.NetworkUtils;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class OfflineExamSyncRepository {
    private static final String TAG = "OfflineExamSyncRepository";
    private static final String QUEUE_FILE = "offline_exam_result_queue.json";
    private static final String QUEUE_FILE_TMP = "offline_exam_result_queue.json.tmp";

    private static OfflineExamSyncRepository instance;

    private static final String DEAD_LETTER_QUEUE_FILE = "offline_exam_dead_letter_queue.json";
    private static final String DEAD_LETTER_QUEUE_FILE_TMP = "offline_exam_dead_letter_queue.json.tmp";

    private final ExamApi examApi;
    private final Gson gson = new Gson();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ExecutorService syncExecutor = Executors.newSingleThreadExecutor();
    private final Object fileLock = new Object();
    private final Object syncLock = new Object();

    public OfflineExamSyncRepository() {
        this(ApiClient.getClient().create(ExamApi.class));
    }

    public OfflineExamSyncRepository(ExamApi examApi) {
        this.examApi = examApi;
    }

    public static synchronized OfflineExamSyncRepository getInstance() {
        if (instance == null) {
            instance = new OfflineExamSyncRepository();
        }
        return instance;
    }

    public void enqueueResult(Context context, QueueItem item) {
        Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            synchronized (fileLock) {
                List<QueueItem> queue = loadQueueSync(appCtx);
                queue.add(item);
                saveQueueSync(appCtx, queue);
            }
            syncPending(appCtx);
            OfflineExamSyncWorker.scheduleSync(appCtx);
        });
    }

    public void syncPending(Context context) {
        Context appCtx = context.getApplicationContext();
        syncExecutor.execute(() -> syncPendingSync(appCtx));
    }

    public boolean syncPendingSync(Context context) {
        Context appCtx = context.getApplicationContext();
        if (!NetworkUtils.isOnline(appCtx)) {
            return false;
        }

        synchronized (syncLock) {
            java.util.Set<String> processedAttemptIds = new java.util.HashSet<>();
            while (true) {
                QueueItem itemToSync = null;
                synchronized (fileLock) {
                    List<QueueItem> queue = loadQueueSync(appCtx);
                    for (QueueItem item : queue) {
                        if (item.localAttemptId != null && !processedAttemptIds.contains(item.localAttemptId)) {
                            itemToSync = item;
                            break;
                        }
                    }
                }
                if (itemToSync == null) {
                    break;
                }

                processedAttemptIds.add(itemToSync.localAttemptId);
                SyncResult result = syncOne(appCtx, itemToSync);

                synchronized (fileLock) {
                    List<QueueItem> queue = loadQueueSync(appCtx);
                    int targetIdx = -1;
                    for (int i = 0; i < queue.size(); i++) {
                        if (java.util.Objects.equals(queue.get(i).localAttemptId, itemToSync.localAttemptId)) {
                            targetIdx = i;
                            break;
                        }
                    }
                    if (targetIdx != -1) {
                        if (result.synced) {
                            queue.remove(targetIdx);
                            saveQueueSync(appCtx, queue);
                        } else if (result.startFailed404) {
                            queue.get(targetIdx).resetAttempts++;
                            if (queue.get(targetIdx).resetAttempts >= 5) {
                                boolean moved = moveToDeadLetterSync(appCtx, queue.get(targetIdx), "Exam unlisted after 5 attempts: HTTP 404");
                                if (moved) {
                                    queue.remove(targetIdx);
                                    saveQueueSync(appCtx, queue);
                                    continue;
                                }
                            }
                            saveQueueSync(appCtx, queue);
                        } else if (result.resetSessionId) {
                            queue.get(targetIdx).resetAttempts++;
                            if (queue.get(targetIdx).resetAttempts >= 5) {
                                boolean moved = moveToDeadLetterSync(appCtx, queue.get(targetIdx), "Exceeded 5 session resets on 404");
                                if (moved) {
                                    queue.remove(targetIdx);
                                    saveQueueSync(appCtx, queue);
                                    continue;
                                }
                            } else {
                                queue.get(targetIdx).remoteSessionId = null;
                            }
                            saveQueueSync(appCtx, queue);
                        } else if (result.remoteSessionId != null && !result.remoteSessionId.equals(queue.get(targetIdx).remoteSessionId)) {
                            queue.get(targetIdx).remoteSessionId = result.remoteSessionId;
                            saveQueueSync(appCtx, queue);
                            // Immediate submit retry by removing from processedAttemptIds
                            processedAttemptIds.remove(itemToSync.localAttemptId);
                        }
                    }
                }
            }

            synchronized (fileLock) {
                return loadQueueSync(appCtx).isEmpty();
            }
        }
    }

    private SyncResult syncOne(Context context, QueueItem item) {
        try {
            Long remoteSessionId = item.remoteSessionId;
            if (remoteSessionId == null) {
                Map<String, Long> body = new HashMap<>();
                body.put("examId", item.examId);
                Response<ApiResponse<ExamSession>> startResponse = examApi.startSession(body).execute();
                if (!startResponse.isSuccessful()) {
                    int code = startResponse.code();
                    if (code == 404) {
                        return SyncResult.startFailed404();
                    }
                    if (isUnrecoverableClientError(code)) {
                        boolean moved = moveToDeadLetterSync(context, item, "startSession HTTP " + code);
                        return moved ? SyncResult.synced() : SyncResult.pending(null);
                    }
                    // 401 Unauthorized, 403 Forbidden, 408, 429, 5xx MUST remain pending to avoid offline data loss
                    return SyncResult.pending(null);
                }
                if (startResponse.body() == null || !startResponse.body().isSuccess()
                        || startResponse.body().getData() == null
                        || startResponse.body().getData().getId() == null) {
                    return SyncResult.pending(null);
                }
                remoteSessionId = startResponse.body().getData().getId();
            }

            ClientSubmitRequest request = new ClientSubmitRequest(
                    item.score,
                    item.correctCount,
                    item.totalQuestions,
                    item.timeSpentSeconds,
                    toClientAnswers(item.answers));
            Response<ApiResponse<ExamSession>> submitResponse = examApi
                    .submitClientResult(remoteSessionId, request)
                    .execute();
            int submitCode = submitResponse.code();
            if (submitResponse.isSuccessful() || submitCode == 409) {
                return SyncResult.synced();
            }
            if (submitCode == 404) {
                // Session expired or invalidated on server -> reset remoteSessionId in queue so next attempt creates a fresh session
                Log.w(TAG, "Remote session " + remoteSessionId + " expired/not found (404), resetting session id for attempt " + item.localAttemptId);
                return SyncResult.resetSession();
            }
            if (isUnrecoverableClientError(submitCode)) {
                boolean moved = moveToDeadLetterSync(context, item, "submitResult HTTP " + submitCode);
                return moved ? SyncResult.synced() : SyncResult.pending(remoteSessionId);
            }
            return SyncResult.pending(remoteSessionId);
        } catch (IOException e) {
            Log.w(TAG, "syncOne network/io failed", e);
            return SyncResult.pending(item.remoteSessionId);
        }
    }

    boolean moveToDeadLetterSync(Context context, QueueItem item, String reason) {
        Log.e(TAG, "Moving offline attempt " + item.localAttemptId + " to dead-letter queue: " + reason);
        synchronized (fileLock) {
            List<QueueItem> dlq = loadDeadLetterSync(context);
            for (QueueItem existing : dlq) {
                if (java.util.Objects.equals(existing.localAttemptId, item.localAttemptId)) {
                    return true; // Already recorded in DLQ, deduplicated
                }
            }
            dlq.add(item);
            return saveDeadLetterSync(context, dlq);
        }
    }

    public List<QueueItem> loadDeadLetterSync(Context context) {
        File file = new File(context.getFilesDir(), DEAD_LETTER_QUEUE_FILE);
        if (!file.exists()) return new ArrayList<>();
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<List<QueueItem>>() {}.getType();
            List<QueueItem> queue = gson.fromJson(reader, type);
            return queue != null ? queue : new ArrayList<>();
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            Log.w(TAG, "loadDeadLetterSync failed", e);
            return new ArrayList<>();
        }
    }

    private boolean saveDeadLetterSync(Context context, List<QueueItem> queue) {
        File tmp = new File(context.getFilesDir(), DEAD_LETTER_QUEUE_FILE_TMP);
        File target = new File(context.getFilesDir(), DEAD_LETTER_QUEUE_FILE);
        try (FileOutputStream fos = new FileOutputStream(tmp);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            gson.toJson(queue, writer);
            writer.flush();
            fos.getFD().sync();
        } catch (IOException e) {
            Log.e(TAG, "saveDeadLetterSync write failed", e);
            return false;
        }
        try {
            java.nio.file.Files.move(tmp.toPath(), target.toPath(),
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            try {
                java.nio.file.Files.move(tmp.toPath(), target.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException ex) {
                Log.e(TAG, "saveDeadLetterSync move failed", ex);
                return false;
            }
        } catch (IOException e) {
            Log.e(TAG, "saveDeadLetterSync move failed", e);
            return false;
        }
    }

    /**
     * True ONLY for unprocessable entity (422 invalid payload format).
     * 400, 404, 401, 403 MUST remain pending to protect student submissions against temporary server bugs/unlisting.
     */
    boolean isUnrecoverableClientError(int code) {
        return code == 422;
    }

    private List<ClientSubmitRequest.AnswerItem> toClientAnswers(List<Answer> answers) {
        List<ClientSubmitRequest.AnswerItem> result = new ArrayList<>();
        if (answers == null) return result;
        for (Answer answer : answers) {
            result.add(new ClientSubmitRequest.AnswerItem(
                    answer.questionId,
                    answer.selectedOptionId,
                    answer.questionOrder,
                    answer.bookmarked));
        }
        return result;
    }

    private List<QueueItem> loadQueueSync(Context context) {
        File file = new File(context.getFilesDir(), QUEUE_FILE);
        if (!file.exists()) return new ArrayList<>();
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<List<QueueItem>>() {}.getType();
            List<QueueItem> queue = gson.fromJson(reader, type);
            return queue != null ? queue : new ArrayList<>();
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            Log.w(TAG, "loadQueueSync failed", e);
            return new ArrayList<>();
        }
    }

    private boolean saveQueueSync(Context context, List<QueueItem> queue) {
        File tmp = new File(context.getFilesDir(), QUEUE_FILE_TMP);
        File target = new File(context.getFilesDir(), QUEUE_FILE);
        try (FileOutputStream fos = new FileOutputStream(tmp);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            gson.toJson(queue, writer);
            writer.flush();
            fos.getFD().sync();
        } catch (IOException e) {
            Log.e(TAG, "saveQueueSync write failed", e);
            return false;
        }
        try {
            java.nio.file.Files.move(tmp.toPath(), target.toPath(),
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            try {
                java.nio.file.Files.move(tmp.toPath(), target.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException ex) {
                Log.e(TAG, "saveQueueSync move failed", ex);
                return false;
            }
        } catch (IOException e) {
            Log.e(TAG, "saveQueueSync move failed", e);
            return false;
        }
    }

    public static QueueItem item(
            long examId,
            double score,
            int correctCount,
            int totalQuestions,
            int timeSpentSeconds,
            List<Answer> answers
    ) {
        QueueItem item = new QueueItem();
        item.localAttemptId = System.currentTimeMillis() + "-" + examId;
        item.examId = examId;
        item.score = score;
        item.correctCount = correctCount;
        item.totalQuestions = totalQuestions;
        item.timeSpentSeconds = timeSpentSeconds;
        item.answers = answers;
        return item;
    }

    public static Answer answer(long questionId, Long selectedOptionId, Integer questionOrder, boolean bookmarked) {
        Answer answer = new Answer();
        answer.questionId = questionId;
        answer.selectedOptionId = selectedOptionId;
        answer.questionOrder = questionOrder;
        answer.bookmarked = bookmarked;
        return answer;
    }

    public static class QueueItem {
        String localAttemptId;
        Long remoteSessionId;
        long examId;
        double score;
        int correctCount;
        int totalQuestions;
        int timeSpentSeconds;
        int resetAttempts;
        List<Answer> answers;
    }

    public static class Answer {
        long questionId;
        Long selectedOptionId;
        Integer questionOrder;
        boolean bookmarked;
    }

    private static class SyncResult {
        final boolean synced;
        final Long remoteSessionId;
        final boolean resetSessionId;
        final boolean startFailed404;

        private SyncResult(boolean synced, Long remoteSessionId, boolean resetSessionId, boolean startFailed404) {
            this.synced = synced;
            this.remoteSessionId = remoteSessionId;
            this.resetSessionId = resetSessionId;
            this.startFailed404 = startFailed404;
        }

        static SyncResult synced() { return new SyncResult(true, null, false, false); }
        static SyncResult pending(Long remoteSessionId) { return new SyncResult(false, remoteSessionId, false, false); }
        static SyncResult resetSession() { return new SyncResult(false, null, true, false); }
        static SyncResult startFailed404() { return new SyncResult(false, null, false, true); }
    }
}
