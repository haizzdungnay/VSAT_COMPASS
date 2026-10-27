package com.example.v_sat_compass.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.ExamApi;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.ClientSubmitRequest;
import com.example.v_sat_compass.data.model.ExamSession;
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

    private final ExamApi examApi;
    private final Gson gson = new Gson();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Object fileLock = new Object();

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
        });
    }

    public void syncPending(Context context) {
        Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            if (!NetworkUtils.isOnline(appCtx)) {
                return;
            }
            synchronized (fileLock) {
                List<QueueItem> queue = loadQueueSync(appCtx);
                boolean changed = false;
                Iterator<QueueItem> iterator = queue.iterator();
                while (iterator.hasNext()) {
                    QueueItem item = iterator.next();
                    SyncResult result = syncOne(item);
                    if (result.synced) {
                        iterator.remove();
                        changed = true;
                    } else if (result.remoteSessionId != null
                            && !result.remoteSessionId.equals(item.remoteSessionId)) {
                        item.remoteSessionId = result.remoteSessionId;
                        changed = true;
                        break;
                    } else {
                        break;
                    }
                }
                if (changed) {
                    saveQueueSync(appCtx, queue);
                }
            }
        });
    }

    private SyncResult syncOne(QueueItem item) {
        try {
            Long remoteSessionId = item.remoteSessionId;
            if (remoteSessionId == null) {
                Map<String, Long> body = new HashMap<>();
                body.put("examId", item.examId);
                Response<ApiResponse<ExamSession>> startResponse = examApi.startSession(body).execute();
                if (!startResponse.isSuccessful() || startResponse.body() == null
                        || !startResponse.body().isSuccess() || startResponse.body().getData() == null
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
            if (submitResponse.isSuccessful()) {
                return SyncResult.synced();
            }
            if (submitResponse.code() == 409) {
                return SyncResult.synced();
            }
            return SyncResult.pending(remoteSessionId);
        } catch (IOException e) {
            Log.w(TAG, "syncOne network/io failed", e);
            return SyncResult.pending(item.remoteSessionId);
        }
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

    private void saveQueueSync(Context context, List<QueueItem> queue) {
        File tmp = new File(context.getFilesDir(), QUEUE_FILE_TMP);
        File target = new File(context.getFilesDir(), QUEUE_FILE);
        try (FileOutputStream fos = new FileOutputStream(tmp);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            gson.toJson(queue, writer);
            writer.flush();
            fos.getFD().sync();
        } catch (IOException e) {
            Log.e(TAG, "saveQueueSync write failed", e);
            return;
        }
        if (!tmp.renameTo(target)) {
            Log.e(TAG, "saveQueueSync rename failed");
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

        private SyncResult(boolean synced, Long remoteSessionId) {
            this.synced = synced;
            this.remoteSessionId = remoteSessionId;
        }

        static SyncResult synced() { return new SyncResult(true, null); }
        static SyncResult pending(Long remoteSessionId) { return new SyncResult(false, remoteSessionId); }
    }
}
