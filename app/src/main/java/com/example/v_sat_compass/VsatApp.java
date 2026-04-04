package com.example.v_sat_compass;

import android.app.Application;

public class VsatApp extends Application {
    private static VsatApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static VsatApp getInstance() {
        return instance;
    }
}
