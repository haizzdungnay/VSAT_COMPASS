package com.example.v_sat_compass.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.v_sat_compass.MainActivity;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.ui.auth.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainHandler.postDelayed(() -> new Thread(() -> {
            boolean loggedIn = ApiClient.isLoggedIn();
            mainHandler.post(() -> navigate(loggedIn));
        }).start(), 1000);
    }

    private void navigate(boolean loggedIn) {
        if (isFinishing()) return;
        if (loggedIn) {
            startActivity(new Intent(this, MainActivity.class));
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }
}
