package com.example.v_sat_compass.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.v_sat_compass.data.repository.OfflineExamSyncRepository;

/**
 * Background worker to automatically sync offline exam submissions when network is restored.
 */
public class OfflineExamSyncWorker extends Worker {

    private static final String TAG = "OfflineExamSyncWorker";
    public static final String UNIQUE_WORK_NAME = "vsat_offline_exam_sync";

    public OfflineExamSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void scheduleSync(Context context) {
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(OfflineExamSyncWorker.class)
                    .setConstraints(constraints)
                    .addTag(UNIQUE_WORK_NAME)
                    .build();

            WorkManager.getInstance(context.getApplicationContext())
                    .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to enqueue WorkManager sync task", t);
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            boolean allSynced = OfflineExamSyncRepository.getInstance().syncPendingSync(getApplicationContext());
            return allSynced ? Result.success() : Result.retry();
        } catch (Throwable t) {
            Log.e(TAG, "Error in offline exam background sync", t);
            return Result.retry();
        }
    }
}
