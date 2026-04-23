package com.example.v_sat_compass;

import android.app.Application;
import android.os.StrictMode;
import android.util.Log;

public class VsatApp extends Application {

    private static final String TAG = "VsatApp";
    private static VsatApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        if (BuildConfig.DEBUG) {
            enableStrictMode();
        }
    }

    /** StrictMode chỉ bật trong debug build để phát hiện disk I/O trên main thread. */
    private void enableStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build());
        Log.d(TAG, "StrictMode enabled (debug build)");
    }

    public static VsatApp getInstance() {
        return instance;
    }
}
